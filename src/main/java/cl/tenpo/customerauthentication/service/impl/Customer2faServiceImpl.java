package cl.tenpo.customerauthentication.service.impl;

import cl.tenpo.customerauthentication.api.dto.*;
import cl.tenpo.customerauthentication.database.repository.CustomerTransactionContextRespository;
import cl.tenpo.customerauthentication.dto.CustomerTransactionContextDTO;
import cl.tenpo.customerauthentication.dto.JwtDTO;
import cl.tenpo.customerauthentication.exception.TenpoException;
import cl.tenpo.customerauthentication.externalservice.cards.CardRestClient;
import cl.tenpo.customerauthentication.externalservice.kafka.EventProducerService;
import cl.tenpo.customerauthentication.externalservice.kafka.dto.LockUnlockUserDto;
import cl.tenpo.customerauthentication.externalservice.login.LoginRestClient;
import cl.tenpo.customerauthentication.externalservice.login.dto.LoginRequestDTO;
import cl.tenpo.customerauthentication.externalservice.login.dto.LoginResponseDTO;
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
import cl.tenpo.customerauthentication.properties.CustomerTransactionContextProperties;
import cl.tenpo.customerauthentication.properties.NotificationMailProperties;
import cl.tenpo.customerauthentication.service.Customer2faService;
import cl.tenpo.customerauthentication.service.CustomerChallengeService;
import cl.tenpo.customerauthentication.util.JwtUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.text.ParseException;
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

    @Autowired
    private LoginRestClient loginRestClient;

    @Autowired
    private EventProducerService eventProducerService;


    @Override
    public LoginResponseDTO login(CustomerLoginRequest request) throws IOException, ParseException {

        if (request.getEmail() == null || request.getEmail().isEmpty()) {
            throw new TenpoException(HttpStatus.BAD_REQUEST, MISSING_PARAMETERS);
        }
        if (request.getClave() == null || request.getClave().isEmpty()) {
            throw new TenpoException(HttpStatus.BAD_REQUEST, MISSING_PARAMETERS);
        }
        if (request.getPan() == null || request.getPan().isEmpty()) {
            throw new TenpoException(HttpStatus.BAD_REQUEST, MISSING_PARAMETERS);
        }
        if(request.getEmail().equalsIgnoreCase("bloqueado@bloqueado.cl") && request.getClave().equalsIgnoreCase("1111")) {
            throw new TenpoException(HttpStatus.UNPROCESSABLE_ENTITY, BLOCKED_PASSWORD);
        }
        //Try Login in AZ AD
        log.info("[login] Try to login into azure ad");
        LoginResponseDTO tokenResponse = loginRestClient.login(LoginRequestDTO.builder()
                .email(request.getEmail())
                .password(request.getClave())
                .app("WEBPAY")
                .build());
        log.info("[login] Login succes");
        JwtDTO jwtDTO = JwtUtil.parseJWT(tokenResponse.getAccessToken());
        log.info("[login] Token Parsed");
        UserResponse userResponseDto = userRestClient.getUserByProvider(jwtDTO.getOid())
                .orElseThrow(() -> new TenpoException(HttpStatus.NOT_FOUND, INVALID_CREDENTIALS));

        log.info("[login] Find User by provider");
        // Verificacion de usuario

        if(!userResponseDto.getState().equals(UserStateType.ACTIVE)) {
            log.error("[login] Cliente no activo");
            throw new TenpoException(HttpStatus.NOT_FOUND, INVALID_CREDENTIALS);
        }
        // Si la verificac
        log.info("[login] Tarjeta verifica tarjeta");
        cardRestClient.checkIfCardBelongsToUser(userResponseDto.getId(),request.getPan());
        log.info("[login] User+Pan OK");
        return tokenResponse;
    }

    @Override
    public void createChallenge(UUID userId, CreateChallengeRequest request) throws JsonProcessingException {
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
    public ValidateChallengeResponse validateChallenge(UUID userId, ValidateChallengeRequest request) throws JsonProcessingException {
        log.info(String.format("[validateChallenge] Validate challenge request for user %s [%s]", userId, request));

        //Verifica que exista ese context id
        Optional<CustomerTransactionContextDTO> customerTransactionContextDTO = customerChallengeService.findByExternalId(request.getExternalId());
        if (!customerTransactionContextDTO.isPresent()) {
            log.error("[validateChallenge] No se encontr?? el external_id. Llame primero a POST");
            throw new TenpoException(HttpStatus.NOT_FOUND, EXTERNAL_ID_NOT_FOUND);
        }

        // Validar que el usuario no esta bloqueado
        UserResponse userResponse = getAndValidateUser(userId);
        log.info("[validateChallenge] Usuario validado: [{}]", userResponse.getTributaryIdentifier());

        // Valida el estado de la trx
        validateTransactionContextStatus(customerTransactionContextDTO.get(), true);
        validateTransactionAttempts(userResponse.getEmail(), customerTransactionContextDTO.get());
        log.info("[validateChallenge] Transaccion validada: [{}]", customerTransactionContextDTO.get().getId());


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
            if(customerTransactionContextDTO.isPresent()){
                validateTransactionAttempts(userResponse.getEmail(),
                        customerTransactionContextDTO.get());
            }

            return ValidateChallengeResponse.builder()
                    .result(ChallengeResult.AUTH_FALLIDA)
                    .externalId(customerTransactionContextDTO.get().getExternalId()).build();
        }
    }

    @Override
    public AbortChallengeResponse abortChallenge(UUID userId, AbortChallengeRequest request) throws JsonProcessingException {

        CustomerTransactionContextDTO customerTransactionContextDTO = customerChallengeService.findByExternalId(request.getExternalId())
                .orElseThrow(() -> new TenpoException(HttpStatus.NOT_FOUND, EXTERNAL_ID_NOT_FOUND));
               // Valida el estado de la trx, excepto si ya esta cancelada

        // Validar que el usuario no esta bloqueado
        UserResponse userResponse = getAndValidateUser(userId);
        log.info("[abortResponse] Usuario validado: [{}]", userResponse.getTributaryIdentifier());

        validateTransactionContextStatus(customerTransactionContextDTO, false);
        validateTransactionAttempts(userResponse.getEmail(), customerTransactionContextDTO);
        log.info("[abortResponse] Transaccion validada: [{}]", customerTransactionContextDTO.getId());


        // Marcar estado como cancelado
        customerTransactionContextDTO = customerChallengeService.updateTransactionContextStatus(customerTransactionContextDTO.getId(), CustomerTransactionStatus.CANCEL)
                .orElseThrow(() -> new TenpoException(HttpStatus.NOT_FOUND, EXTERNAL_ID_NOT_FOUND));

        return AbortChallengeResponse.builder()
               .externalId(customerTransactionContextDTO.getExternalId())
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

    @Override
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

    }

    @Override
    public void validateTransactionAttempts(String email, CustomerTransactionContextDTO transactionContextDTO) throws JsonProcessingException {
        // Retorno cuando la trx fue rechazada por intentos
        if (transactionContextDTO.getStatus().equals(CustomerTransactionStatus.REJECTED)) {
            throw new TenpoException(HttpStatus.UNPROCESSABLE_ENTITY, BLOCKED_PASSWORD);
        }

        // Verifica la cantidad de intentos y actualiza status de la trx
        if (transactionContextDTO.getAttempts() >= transactionContextProperties.getPasswordAttempts()) {
            log.info("[validateTransactionAttempts] Rejected by Attempts");
            customerChallengeService.updateTransactionContextStatus(transactionContextDTO.getId(), CustomerTransactionStatus.REJECTED);
            eventProducerService.sendLockEvent(LockUnlockUserDto
                    .builder()
                    .email(email)
                    .attemp(4)
                    .build());
            log.info("[validateTransactionAttempts] Sending Lock Password Evt");
            throw new TenpoException(HttpStatus.UNPROCESSABLE_ENTITY, BLOCKED_PASSWORD);
        }
    }

}
