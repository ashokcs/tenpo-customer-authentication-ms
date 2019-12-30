package cl.tenpo.customerauthentication.service.impl;

import cl.tenpo.customerauthentication.api.dto.*;
import cl.tenpo.customerauthentication.component.AzureClient;
import cl.tenpo.customerauthentication.dto.CustomerTransactionContextDTO;
import cl.tenpo.customerauthentication.dto.JwtDTO;
import cl.tenpo.customerauthentication.exception.TenpoException;
import cl.tenpo.customerauthentication.externalservice.azure.dto.TokenResponse;
import cl.tenpo.customerauthentication.externalservice.cards.CardRestClient;
import cl.tenpo.customerauthentication.constants.NotificationsProperties;
import cl.tenpo.customerauthentication.constants.ErrorCode;
import cl.tenpo.customerauthentication.database.repository.CustomerTransactionContextRespository;
import cl.tenpo.customerauthentication.externalservice.notification.NotificationRestClient;
import cl.tenpo.customerauthentication.externalservice.notification.dto.EmailDto;
import cl.tenpo.customerauthentication.externalservice.notification.dto.NotificationEventType;
import cl.tenpo.customerauthentication.externalservice.notification.dto.TwoFactorPushRequest;
import cl.tenpo.customerauthentication.externalservice.user.UserRestClient;
import cl.tenpo.customerauthentication.externalservice.user.dto.UserResponse;
import cl.tenpo.customerauthentication.externalservice.user.dto.UserStateType;
import cl.tenpo.customerauthentication.externalservice.verifier.VerifierRestClient;
import cl.tenpo.customerauthentication.model.ChallengeResult;
import cl.tenpo.customerauthentication.model.ChallengeType;
import cl.tenpo.customerauthentication.model.CustomerTransactionStatus;
import cl.tenpo.customerauthentication.model.NewCustomerChallenge;
import cl.tenpo.customerauthentication.properties.VerifierProps;
import cl.tenpo.customerauthentication.service.Customer2faService;
import cl.tenpo.customerauthentication.service.CustomerChallengeService;
import cl.tenpo.customerauthentication.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static cl.tenpo.customerauthentication.constants.ErrorCode.*;
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

    @Autowired
    private NotificationsProperties notificationsProperties;

    @Autowired
    private VerifierRestClient verifierRestClient;


    @Override
    public TokenResponse login(CustomerLoginRequest request) {
        TokenResponse tokenResponse;
        Optional<UserResponse> userResponseDto;
        try {
            //Try Login in AZ AD
            log.info("[login] Try to login into azure ad");
            tokenResponse = azureClient.loginUser(request.getEmail(),request.getPassword());
            log.info("[login] Login succes");
            JwtDTO jwtDTO = JwtUtil.parseJWT(tokenResponse.getAccessToken());
            log.info("[login] Token Parsed");
            userResponseDto = userRestClient.getUserByProvider(jwtDTO.getOid());
            log.info("[login] Find User by provider");
            // Verificacion de usuario
            if(userResponseDto.isPresent()) {
               if(!userResponseDto.get().getState().equals(UserStateType.ACTIVE)) {
                   log.error("[login] Cliente no activo");
                   throw new TenpoException(HttpStatus.NOT_FOUND, USER_NOT_FOUND_OR_LOCKED, "El cliente no existe o está bloqueado");
               }
            }else {
                log.error("[login] Usuario no existe");
                throw new TenpoException(HttpStatus.NOT_FOUND, USER_NOT_FOUND_OR_LOCKED, "El cliente no existe o está bloqueado");
            }
        } catch (Exception e){
            log.error("[login] Error login on Azure AD");
            throw new TenpoException(HttpStatus.NOT_FOUND, USER_NOT_FOUND_OR_LOCKED, "El cliente no existe o está bloqueado");
        }
        // Si la verificac
        log.info("[login] Tarjeta verifica tarjeta");
        cardRestClient.checkIfCardBelongsToUser(userResponseDto.get().getId(),request.getPan());
        log.info("[login] User+Pan OK");
        return tokenResponse;
    }

    @Override
    public void createChallenge(UUID userId, CreateChallengeRequest request) {
        log.info(String.format("[createChallenge] Challenge request for user %s [%s]", userId, request));

        // Validar que el usuario no esta bloqueado
        UserResponse userResponse = getAndValidateUser(userId);
        log.info("[createChallenge] Usuario validado: [{}]", userResponse.getTributaryIdentifier());

        // Crear el challenge
        NewCustomerChallenge newCustomerChallenge = customerChallengeService.createRequestedChallenge(userId, request);
        log.info(String.format("[createChallenge] Challenge creado: [id:%s][code:XXX%s]", newCustomerChallenge.getChallengeId(), newCustomerChallenge.getCode().substring(3)));

        // Enviar el challenge al usuario
        sendChallenge(newCustomerChallenge, userResponse);
        log.info("[createChallenge] Challenge enviado a usuario.");
    }

    @Override
    public ValidateChallengeResponse validateChallenge(UUID userId, ValidateChallengeRequest request) {
        //Verifica que exista un desafio con este context id
        Optional<CustomerTransactionContextDTO> customerTransactionContextDTO = customerChallengeService.findByExternalId(request.getExternalId());
        if(!customerTransactionContextDTO.isPresent()) {
            log.error("[validateChallenge] No se encontró el external_id. Llame primero a POST");
            throw new TenpoException(HttpStatus.UNPROCESSABLE_ENTITY, EXTERNAL_ID_NOT_FOUND, "No se encontró el external_id. Llame primero a POST");
        }

        // Retorno cuando el desafio es rechazado
        if(customerTransactionContextDTO.get().getStatus().equals(CustomerTransactionStatus.REJECTED)) {
            throw new TenpoException(HttpStatus.UNPROCESSABLE_ENTITY, TRANSACTION_CONTEXT_LOCKED, "Transaccion bloqueada por intentos");
        }

        // Verifica la cantidad de intentos y actualiza status de la trx
        if(customerTransactionContextDTO.get().getAttempts()>4) {
            customerChallengeService.updateTransactionContextStatus(customerTransactionContextDTO.get().getId(),CustomerTransactionStatus.REJECTED);
            throw new TenpoException(HttpStatus.UNPROCESSABLE_ENTITY, TRANSACTION_CONTEXT_LOCKED, "Transaccion bloqueada por intentos");
        }

        // Validar que el usuario no esta bloqueado
        UserResponse userResponse = getAndValidateUser(userId);
        log.info("[validateChallenge] Usuario validado: [{}]", userResponse.getTributaryIdentifier());
        // Verifica el codigo.
        boolean responceVeriverifier = verifierRestClient.validateTwoFactorCode(userId, request.getResponse());

        if(responceVeriverifier) {
            //Actualiza status a TRX authorizada
            customerChallengeService.updateTransactionContextStatus(customerTransactionContextDTO.get().getExternalId(),CustomerTransactionStatus.AUTHORIZED);
            return ValidateChallengeResponse.builder()
                    .result(ChallengeResult.AUTH_EXITOSA)
                    .externalId(customerTransactionContextDTO.get().getExternalId()).build();

        } else {
            //Aumenta numero de intentos y responde AUTH FAIL
            customerChallengeService.addTransactionContextAttempt(customerTransactionContextDTO.get().getId());
            return ValidateChallengeResponse.builder()
                    .result(ChallengeResult.AUTH_FALLIDA)
                    .externalId(customerTransactionContextDTO.get().getExternalId()).build();
        }
    }

    @Override
    public AbortChallengeResponse abortResponse(UUID userId, AbortChallengeRequest request) {

        Optional<CustomerTransactionContextDTO> customerTransactionContextDTO = customerChallengeService.findByExternalId(request.getExternalId());

        if(!customerTransactionContextDTO.isPresent()){
            log.error("[abortResponse] No se encontró el external_id. Llame primero a POST");
            throw new TenpoException(HttpStatus.UNPROCESSABLE_ENTITY, EXTERNAL_ID_NOT_FOUND, "No se encontró el external_id. Llame primero a POST");
        }

        // Validar que el usuario no esta bloqueado
        UserResponse userResponse = getAndValidateUser(userId);
        log.info("[abortResponse] Usuario validado: [{}]", userResponse.getTributaryIdentifier());

        customerTransactionContextDTO = customerChallengeService.updateTransactionContextStatus(request.getExternalId(), CustomerTransactionStatus.CANCEL);

       if(!customerTransactionContextDTO.isPresent()){
            log.error("[abortResponse] No se encontró el external_id. Llame primero a POST");
            throw new TenpoException(HttpStatus.UNPROCESSABLE_ENTITY, EXTERNAL_ID_NOT_FOUND, "No se encontró el external_id. Llame primero a POST");
       }

       return AbortChallengeResponse.builder()
               .externalId(customerTransactionContextDTO.get().getExternalId())
               .result(customerTransactionContextDTO.get().getStatus())
               .build();
    }

    @Override
    public List<String> listChallenge(UUID userId) {
        // Validar que el usuario no esta bloqueado
        UserResponse userResponse = getAndValidateUser(userId);
        log.info("[createChallenge] Usuario validado: [{}]", userResponse.getTributaryIdentifier());
        return Stream.of(ChallengeType.values()).map(Enum::name).collect(Collectors.toList());
    }

    private UserResponse getAndValidateUser(UUID userId) {
        Optional<UserResponse> requestUser;
        try {
            requestUser = userRestClient.getUser(userId);
        } catch (Exception e) {
            throw new TenpoException(HttpStatus.NOT_FOUND, USER_NOT_FOUND_OR_LOCKED, "El cliente no existe o está bloqueado");
        }

        if (!requestUser.isPresent()) {
            throw new TenpoException(HttpStatus.NOT_FOUND, USER_NOT_FOUND_OR_LOCKED, "El cliente no existe o está bloqueado");
        }
        if (!requestUser.get().getState().equals(UserStateType.ACTIVE)) {
            throw new TenpoException(HttpStatus.NOT_FOUND, USER_NOT_FOUND_OR_LOCKED, "El cliente no existe o está bloqueado");
        }

        return requestUser.get();
    }


    private void sendChallenge(NewCustomerChallenge newCustomerChallenge, UserResponse userResponse) {

        try {
            switch (newCustomerChallenge.getChallengeType()) {
                case OTP_MAIL: {
                    EmailDto emailDto = EmailDto.builder()
                            .from(notificationsProperties.getTwoFactorMailFrom())
                            .to(userResponse.getEmail())
                            .referenceId(newCustomerChallenge.getChallengeId().toString())
                            .subject(notificationsProperties.getTwoFactorMailSubject())
                            .template(notificationsProperties.getTwoFactorMailTemplate())
                            .params(buildTwoFactorMailParam(
                                    userResponse.getFirstName(),
                                    newCustomerChallenge.getCode()))
                            .build();
                    notificationRestClient.sendEmail(emailDto);
                    break;
                }
                case APP: {
                    // Todo: no se implementa por ahora
                    break;
                }
                case OTP_SMS: {
                    TwoFactorPushRequest twoFactorPushRequest = TwoFactorPushRequest.builder()
                            .userId(userResponse.getId())
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
                            .pusherEvent(NotificationEventType.VERIFICATION_CODE)
                            .messageType(PUSH_NOTIFICATION)
                            .verificationCode(newCustomerChallenge.getCode())
                            .build();
                    notificationRestClient.sendMessagePush(twoFactorPushRequest);
                    break;
                }
            }
        } catch (Exception e) {
            throw new TenpoException(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.NOTIFICATION_ERROR);
        }
    }

    private Map<String, String> buildTwoFactorMailParam(String name, String twoFactorCode) {
        Map<String, String> mailParam = new HashMap<>();
        mailParam.put("user_name", name);
        mailParam.put("-code-", twoFactorCode);
        return mailParam;
    }
}
