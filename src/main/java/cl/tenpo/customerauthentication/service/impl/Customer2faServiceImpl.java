package cl.tenpo.customerauthentication.service.impl;

import cl.tenpo.customerauthentication.api.dto.*;
import cl.tenpo.customerauthentication.component.AzureClient;
import cl.tenpo.customerauthentication.dto.CustomerTransactionContextDTO;
import cl.tenpo.customerauthentication.dto.JwtDTO;
import cl.tenpo.customerauthentication.exception.TenpoException;
import cl.tenpo.customerauthentication.externalservice.azure.dto.TokenResponse;
import cl.tenpo.customerauthentication.externalservice.cards.CardRestClient;
import cl.tenpo.customerauthentication.properties.CustomerTransactionContextProperties;
import cl.tenpo.customerauthentication.properties.NotificationMailProperties;
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
import cl.tenpo.customerauthentication.service.Customer2faService;
import cl.tenpo.customerauthentication.service.CustomerChallengeService;
import cl.tenpo.customerauthentication.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
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
    private NotificationMailProperties notificationMailProperties;

    @Autowired
    private VerifierRestClient verifierRestClient;

    @Autowired
    private CustomerTransactionContextProperties transactionContextProperties;

    @Override
    public TokenResponse login(CustomerLoginRequest request) {
        if (request.getEmail() == null || request.getEmail().isEmpty()) {
            throw new TenpoException(HttpStatus.BAD_REQUEST, MISSING_PARAMETERS);
        }
        if (request.getClave() == null || request.getClave().isEmpty()) {
            throw new TenpoException(HttpStatus.BAD_REQUEST, MISSING_PARAMETERS);
        }
        if (request.getPan() == null || request.getPan().isEmpty()) {
            throw new TenpoException(HttpStatus.BAD_REQUEST, MISSING_PARAMETERS);
        }

        TokenResponse tokenResponse;
        Optional<UserResponse> userResponseDto;
        try {
            //Try Login in AZ AD
            log.info("[login] Try to login into azure ad");
            tokenResponse = azureClient.loginUser(request.getEmail(),request.getClave());
            log.info("[login] Login succes");
            JwtDTO jwtDTO = JwtUtil.parseJWT(tokenResponse.getAccessToken());
            log.info("[login] Token Parsed");
            userResponseDto = userRestClient.getUserByProvider(jwtDTO.getOid());
            log.info("[login] Find User by provider");
            // Verificacion de usuario
            if(userResponseDto.isPresent()) {
               if(!userResponseDto.get().getState().equals(UserStateType.ACTIVE)) {
                   log.error("[login] Cliente no activo");
                   throw new TenpoException(HttpStatus.NOT_FOUND, INVALID_CREDENTIALS);
               }
            }else {
                log.error("[login] Usuario no existe");
                throw new TenpoException(HttpStatus.NOT_FOUND, INVALID_CREDENTIALS);
            }
        } catch (Exception e){
            log.error("[login] Error login on Azure AD");
            throw new TenpoException(HttpStatus.NOT_FOUND, INVALID_CREDENTIALS);
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
        log.info(String.format("[validateChallenge] Validate challenge request for user %s [%s]", userId, request));

        //Verifica que exista ese context id
        Optional<CustomerTransactionContextDTO> customerTransactionContextDTO = customerChallengeService.findByExternalId(request.getExternalId());
        if (!customerTransactionContextDTO.isPresent()) {
            log.error("[validateChallenge] No se encontró el external_id. Llame primero a POST");
            throw new TenpoException(HttpStatus.NOT_FOUND, EXTERNAL_ID_NOT_FOUND);
        }

        // Valida el estado de la trx
        validateTransactionContextStatus(customerTransactionContextDTO.get(), true);
        log.info("[validateChallenge] Transaccion validada: [{}]", customerTransactionContextDTO.get().getId());

        // Validar que el usuario no esta bloqueado
        UserResponse userResponse = getAndValidateUser(userId);
        log.info("[validateChallenge] Usuario validado: [{}]", userResponse.getTributaryIdentifier());

        // Verifica el codigo.
        boolean verifierResponse = verifierRestClient.validateTwoFactorCode(userId, request.getResponse());

        if (verifierResponse) {
            //Actualiza status a TRX authorizada
            customerChallengeService.updateTransactionContextStatus(customerTransactionContextDTO.get().getId(),CustomerTransactionStatus.AUTHORIZED);
            return ValidateChallengeResponse.builder()
                    .result(ChallengeResult.AUTH_EXITOSA)
                    .externalId(customerTransactionContextDTO.get().getExternalId()).build();

        } else {
            //Aumenta numero de intentos y responde AUTH FAIL
            customerTransactionContextDTO = customerChallengeService.addTransactionContextAttempt(customerTransactionContextDTO.get().getId());

            // Verifica la cantidad de intentos y actualiza status de la trx
            customerTransactionContextDTO.ifPresent(this::validateTransactionAttempts);

            return ValidateChallengeResponse.builder()
                    .result(ChallengeResult.AUTH_FALLIDA)
                    .externalId(customerTransactionContextDTO.get().getExternalId()).build();
        }
    }

    @Override
    public AbortChallengeResponse abortChallenge(UUID userId, AbortChallengeRequest request) {

        Optional<CustomerTransactionContextDTO> customerTransactionContextDTO = customerChallengeService.findByExternalId(request.getExternalId());
        if(!customerTransactionContextDTO.isPresent()){
            log.error("[abortResponse] No se encontró el external_id. Llame primero a POST");
            throw new TenpoException(HttpStatus.NOT_FOUND, EXTERNAL_ID_NOT_FOUND);
        }

        // Valida el estado de la trx, excepto si ya esta cancelada
        validateTransactionContextStatus(customerTransactionContextDTO.get(), false);
        log.info("[abortResponse] Transaccion validada: [{}]", customerTransactionContextDTO.get().getId());

        // Validar que el usuario no esta bloqueado
        UserResponse userResponse = getAndValidateUser(userId);
        log.info("[abortResponse] Usuario validado: [{}]", userResponse.getTributaryIdentifier());

        // Marcar estado como cancelado
        customerTransactionContextDTO = customerChallengeService.updateTransactionContextStatus(customerTransactionContextDTO.get().getId(), CustomerTransactionStatus.CANCEL);

        return AbortChallengeResponse.builder()
               .externalId(customerTransactionContextDTO.get().getExternalId())
               .result(ChallengeResult.CANCELADO)
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
            throw new TenpoException(HttpStatus.UNAUTHORIZED, INVALID_TOKEN);
        }

        if (!requestUser.isPresent()) {
            throw new TenpoException(HttpStatus.UNAUTHORIZED, INVALID_TOKEN);
        }
        if (!requestUser.get().getState().equals(UserStateType.ACTIVE)) {
            throw new TenpoException(HttpStatus.UNAUTHORIZED, INVALID_TOKEN);
        }

        return requestUser.get();
    }

    private void sendChallenge(NewCustomerChallenge newCustomerChallenge, UserResponse userResponse) {

        try {
            switch (newCustomerChallenge.getChallengeType()) {
                case OTP_MAIL: {
                    EmailDto emailDto = EmailDto.builder()
                            .from(notificationMailProperties.getTwoFactorMailFrom())
                            .to(userResponse.getEmail())
                            .referenceId(newCustomerChallenge.getChallengeId().toString())
                            .subject(notificationMailProperties.getTwoFactorMailSubject())
                            .template(notificationMailProperties.getTwoFactorMailTemplate())
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
            log.error("[sendChallenge] Error al enviar notificacion");
        }
    }

    private Map<String, String> buildTwoFactorMailParam(String name, String twoFactorCode) {
        Map<String, String> mailParam = new HashMap<>();
        mailParam.put("user_name", name);
        mailParam.put("-code-", twoFactorCode);
        return mailParam;
    }

    public void validateTransactionContextStatus(CustomerTransactionContextDTO transactionContextDTO, boolean validateCanceled) {

        // Retorno cuando la trx ya fue autorizada previamente
        if (transactionContextDTO.getStatus().equals(CustomerTransactionStatus.AUTHORIZED)) {
            throw new TenpoException(HttpStatus.UNPROCESSABLE_ENTITY, TRANSACTION_CONTEXT_CLOSED);
        }

        // Retorno cuando la trx ya fue cancelada
        if (transactionContextDTO.getStatus().equals(CustomerTransactionStatus.CANCEL) && validateCanceled) {
            throw new TenpoException(HttpStatus.UNPROCESSABLE_ENTITY, TRANSACTION_CONTEXT_CANCELED);
        }

        // Retorno cuando la trx ha expirado
        if (transactionContextDTO.getStatus().equals(CustomerTransactionStatus.EXPIRED)) {
            throw new TenpoException(HttpStatus.UNPROCESSABLE_ENTITY, TRANSACTION_CONTEXT_EXPIRED);
        }

        // Verifica duracion de la trx y la expira si es necesario
        if (LocalDateTime.now(ZoneId.of("UTC")).isAfter(transactionContextDTO.getCreated().plusMinutes(transactionContextProperties.getExpirationTimeInMinutes()))) {
            customerChallengeService.updateTransactionContextStatus(transactionContextDTO.getId(), CustomerTransactionStatus.EXPIRED);
            throw new TenpoException(HttpStatus.UNPROCESSABLE_ENTITY, TRANSACTION_CONTEXT_EXPIRED);
        }

        // Verifica numero de intentos de validar el codigo
        validateTransactionAttempts(transactionContextDTO);
    }

    private void validateTransactionAttempts(CustomerTransactionContextDTO transactionContextDTO) {
        // Retorno cuando la trx fue rechazada por intentos
        if (transactionContextDTO.getStatus().equals(CustomerTransactionStatus.REJECTED)) {
            throw new TenpoException(HttpStatus.UNPROCESSABLE_ENTITY, BLOCKED_PASSWORD);
        }

        // Verifica la cantidad de intentos y actualiza status de la trx
        if (transactionContextDTO.getAttempts() >= transactionContextProperties.getPasswordAttempts()) {
            customerChallengeService.updateTransactionContextStatus(transactionContextDTO.getId(), CustomerTransactionStatus.REJECTED);
            throw new TenpoException(HttpStatus.UNPROCESSABLE_ENTITY, BLOCKED_PASSWORD);
        }
    }

}
