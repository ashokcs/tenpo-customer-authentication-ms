package cl.tenpo.customerauthentication.service.impl;

import cl.tenpo.customerauthentication.api.dto.*;
import cl.tenpo.customerauthentication.component.AzureClient;
import cl.tenpo.customerauthentication.dto.JwtDTO;
import cl.tenpo.customerauthentication.exception.TenpoException;
import cl.tenpo.customerauthentication.externalservice.azure.dto.TokenResponse;
import cl.tenpo.customerauthentication.externalservice.cards.CardRestClient;
import cl.tenpo.customerauthentication.constants.CustomerAuthenticationConstants;
import cl.tenpo.customerauthentication.constants.ErrorCode;
import cl.tenpo.customerauthentication.database.repository.CustomerTransactionContextRespository;
import cl.tenpo.customerauthentication.exception.TenpoException;
import cl.tenpo.customerauthentication.externalservice.azure.dto.TokenResponse;
import cl.tenpo.customerauthentication.externalservice.notification.NotificationRestClient;
import cl.tenpo.customerauthentication.externalservice.notification.dto.EmailV2Dto;
import cl.tenpo.customerauthentication.externalservice.notification.dto.NotificationEventType;
import cl.tenpo.customerauthentication.externalservice.notification.dto.TwoFactorPushRequest;
import cl.tenpo.customerauthentication.externalservice.user.UserRestClient;
import cl.tenpo.customerauthentication.externalservice.user.dto.UserResponse;
import cl.tenpo.customerauthentication.externalservice.user.dto.UserStateType;
import cl.tenpo.customerauthentication.model.ChallengeType;
import cl.tenpo.customerauthentication.model.NewCustomerChallenge;
import cl.tenpo.customerauthentication.service.Customer2faService;
import cl.tenpo.customerauthentication.service.CustomerChallengeService;
import cl.tenpo.customerauthentication.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static cl.tenpo.customerauthentication.externalservice.notification.dto.MessageType.PUSH_NOTIFICATION;
import static cl.tenpo.customerauthentication.externalservice.notification.dto.MessageType.SMS;

@Service
@Slf4j
public class Customer2faServiceImpl implements Customer2faService {

    @Autowired
    private AzureClient azureClient;

    @Autowired
    private UserRestClient userRestClient;

    @Autowired
    private CardRestClient cardRestClient;
    @Autowired
    private CustomerChallengeService customerChallengeService;

    @Autowired
    private CustomerTransactionContextRespository transactionContextRespository;

    @Autowired
    private NotificationRestClient notificationRestClient;

    @Override
    public TokenResponse login(CustomerLoginRequest request) {
        TokenResponse tokenResponse;
        try {
            //Try Login in AZ AD
            tokenResponse = azureClient.loginUser(request.getEmail(),request.getPassword());
            JwtDTO jwtDTO = JwtUtil.parseJWT(tokenResponse.getAccessToken());
            Optional<UserResponse> userResponseDto = userRestClient.getUserByProvider(jwtDTO.getOid());
            // Verificacion de usuario
            if(userResponseDto.isPresent()){
               if(!userResponseDto.get().getState().equals(UserStateType.ACTIVE)){
                   throw new TenpoException(HttpStatus.NOT_FOUND,"1150","El cliente no existe o está bloqueado");
               }
               // Si la verificac
               cardRestClient.checkIfCardBelongsToUser(userResponseDto.get().getId(),request.getPan());
            }else {
                throw new TenpoException(HttpStatus.NOT_FOUND,"1150","El cliente no existe o está bloqueado");
            }
        } catch (Exception e){
            throw new TenpoException(HttpStatus.NOT_FOUND,"1150","El cliente no existe o está bloqueado");
        }

        return tokenResponse;
    }

    @Override
    public void createChallenge(UUID userId, CreateChallengeRequest request) {

        // Validar que el usuario no esta bloqueado
        UserResponse userResponse = getAndValidateUser(userId);

        // Crear el challenge
        NewCustomerChallenge newCustomerChallenge = customerChallengeService.createRequestedChallenge(userId, request);

        // Enviar el challenge al usuario
        sendChallenge(newCustomerChallenge, userResponse);
    }

    @Override
    public ValidateChallengeResponse validateChallenge(UUID userId, ValidateChallengeRequest request) {

        return null;
    }

    @Override
    public AbortChallengeResponse abortResponse(UUID userId, AbortChallengeRequest request) {
        return null;
    }

    @Override
    public List<String> listChallenge(UUID userId) {
        Optional<UserResponse> userResponseDto;
        try {
            userResponseDto = userRestClient.getUser(userId);
         }catch (Exception e){
            log.error("[listChallenge] Error al verificar usuario");
            throw new TenpoException(HttpStatus.UNPROCESSABLE_ENTITY,"500","Error desconocido a verificar ");
         }
        if(!userResponseDto.isPresent()|| !userResponseDto.get().getState().equals(UserStateType.ACTIVE)){
            throw new TenpoException(HttpStatus.NOT_FOUND,"1150","El cliente no existe o está bloqueado");
        }
        return Stream.of(ChallengeType.values()).map(Enum::name).collect(Collectors.toList());
    }

    private UserResponse getAndValidateUser(UUID userId) {
        Optional<UserResponse> requestUser;
        try {
            requestUser = userRestClient.getUser(userId);
        } catch (Exception e) {
            throw new TenpoException(HttpStatus.UNPROCESSABLE_ENTITY, ErrorCode.USER_NOT_FOUND_OR_LOCKED);
        }

        if (!requestUser.isPresent()) {
            throw new TenpoException(HttpStatus.UNPROCESSABLE_ENTITY, ErrorCode.USER_NOT_FOUND_OR_LOCKED);
        }
        if (!requestUser.get().getState().equals(UserStateType.ACTIVE)) {
            throw new TenpoException(HttpStatus.UNPROCESSABLE_ENTITY, ErrorCode.USER_NOT_FOUND_OR_LOCKED);
        }

        return requestUser.get();
    }


    private void sendChallenge(NewCustomerChallenge newCustomerChallenge, UserResponse userResponse) {

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

        switch (newCustomerChallenge.getChallengeType()) {
            case OTP_MAIL: {
                EmailV2Dto emailV2Dto = EmailV2Dto.builder()
                        .from(CustomerAuthenticationConstants.TWO_FACTOR_MAIL_FROM)
                        .to(userResponse.getEmail())
                        .referenceId(newCustomerChallenge.getChallengeId().toString())
                        .subject(CustomerAuthenticationConstants.TWO_FACTOR_MAIL_SUBJECT)
                        .bcc(new String[]{userResponse.getEmail()})
                        .template(CustomerAuthenticationConstants.TWO_FACTOR_MAIL_TEMPLATE)
                        .params(buildTwoFactorMailParam(
                                userResponse.getFirstName(),
                                String.format("%s %s ", userResponse.getFirstName(), userResponse.getLastName()),
                                newCustomerChallenge.getCode(),
                                newCustomerChallenge.getChallengeId().toString(),
                                LocalDateTime.now(ZoneId.of("America/Santiago")).format(formatter)
                        ))
                        .build();
                notificationRestClient.sendEmailv2(emailV2Dto);
                break;
            }
            case APP: {
                // Todo: no se implementa por ahora
                break;
            }
            case OTP_SMS: {
                TwoFactorPushRequest twoFactorPushRequest = TwoFactorPushRequest.builder()
                        .userId(userResponse.getId())
                        .linkId(newCustomerChallenge.getChallengeId())
                        .pusherEvent(NotificationEventType.VERIFICATION_CODE)
                        .messageType(SMS)
                        .verificationCode(newCustomerChallenge.getCode())
                        .build();
                notificationRestClient.sendMessagePush(twoFactorPushRequest);
                break;
            }
            case OTP_PUSH: {
                TwoFactorPushRequest twoFactorPushRequest = TwoFactorPushRequest.builder()
                        .userId(userResponse.getId())
                        .linkId(newCustomerChallenge.getChallengeId())
                        .pusherEvent(NotificationEventType.VERIFICATION_CODE)
                        .messageType(PUSH_NOTIFICATION)
                        .verificationCode(newCustomerChallenge.getCode())
                        .build();
                notificationRestClient.sendMessagePush(twoFactorPushRequest);
                break;
            }
        }
    }

    private Map<String, String> buildTwoFactorMailParam(String name, String fullName, String twoFactorCode,
                                                        String transactionId, String date) {
        Map<String, String> mailParam = new HashMap<>();
        mailParam.put("{{name}}", name);
        mailParam.put("{{fullName}}", fullName);
        mailParam.put("{{code}}", twoFactorCode);
        mailParam.put("{{transactionId}}", transactionId);
        mailParam.put("{{date}}", date);
        return mailParam;
    }
}
