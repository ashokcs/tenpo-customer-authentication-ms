package cl.tenpo.customerauthentication.integration.service;

import cl.tenpo.customerauthentication.CustomerAuthenticationMsApplicationTests;
import cl.tenpo.customerauthentication.api.dto.CreateChallengeRequest;
import cl.tenpo.customerauthentication.api.dto.ValidateChallengeRequest;
import cl.tenpo.customerauthentication.api.dto.ValidateChallengeResponse;
import cl.tenpo.customerauthentication.constants.ErrorCode;
import cl.tenpo.customerauthentication.database.entity.CustomerTransactionContextEntity;
import cl.tenpo.customerauthentication.database.repository.CustomerChallengeRepository;
import cl.tenpo.customerauthentication.database.repository.CustomerTransactionContextRespository;
import cl.tenpo.customerauthentication.dto.CustomerTransactionContextDTO;
import cl.tenpo.customerauthentication.exception.TenpoException;
import cl.tenpo.customerauthentication.externalservice.kafka.EventProducerService;
import cl.tenpo.customerauthentication.externalservice.kafka.dto.LockUnlockUserDto;
import cl.tenpo.customerauthentication.externalservice.user.UserRestClient;
import cl.tenpo.customerauthentication.externalservice.user.dto.UserResponse;
import cl.tenpo.customerauthentication.externalservice.user.dto.UserStateType;
import cl.tenpo.customerauthentication.externalservice.verifier.VerifierRestClient;
import cl.tenpo.customerauthentication.model.ChallengeResult;
import cl.tenpo.customerauthentication.model.CustomerTransactionStatus;
import cl.tenpo.customerauthentication.properties.CustomerTransactionContextProperties;
import cl.tenpo.customerauthentication.service.Customer2faService;
import cl.tenpo.customerauthentication.service.CustomerChallengeService;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@SpringBootTest
public class Customer2faServiceValidateChallengeTests extends CustomerAuthenticationMsApplicationTests {

    @Autowired
    private CustomerChallengeService customerChallengeService;

    @Autowired
    private CustomerChallengeRepository customerChallengeRepository;

    @Autowired
    private CustomerTransactionContextRespository customerTransactionContextRespository;

    @Autowired
    private Customer2faService customer2faService;

    @Autowired
    private CustomerTransactionContextProperties transactionContextProperties;

    @MockBean
    private UserRestClient userRestClient;

    @MockBean
    private VerifierRestClient verifierRestClient;

    @MockBean
    private EventProducerService eventProducerService;

    @Before
    @After
    public void clearDB() {
        customerChallengeRepository.deleteAll();
        customerTransactionContextRespository.deleteAll();
    }

    @Test(expected = TenpoException.class)
    public void whenTrxNotFound_ThenThrowNotFoundException() {
        ValidateChallengeRequest validateChallengeRequest = new ValidateChallengeRequest();
        validateChallengeRequest.setExternalId(UUID.randomUUID().toString());

        try {
            customer2faService.validateChallenge(UUID.randomUUID(), validateChallengeRequest);
            Assert.fail("Debe tirar tenpo exception");
        } catch (TenpoException te) {
            Assert.assertEquals("Debe ser 404", HttpStatus.NOT_FOUND, te.getCode());
            Assert.assertEquals("Debe ser 1151", ErrorCode.EXTERNAL_ID_NOT_FOUND, te.getErrorCode());
            throw te;
        } catch (Exception e) {
            Assert.fail("Debe tirar tenpo exception");
        }
    }

    @Test(expected = TenpoException.class)
    public void whenTrxAuthorized_ThenThrowException() {
        UUID userId = UUID.randomUUID();
        // Prepare user response OK
        UserResponse userResponse = new UserResponse();
        userResponse.setId(userId);
        userResponse.setState(UserStateType.ACTIVE);
        when(userRestClient.getUser(Mockito.any(UUID.class)))
                .thenReturn(Optional.of(userResponse));

        // Agrega la transaccion ya existente
        CustomerTransactionContextEntity savedTransactionEntity = createTransaction(userId);
        savedTransactionEntity.setStatus(CustomerTransactionStatus.AUTHORIZED);
        customerTransactionContextRespository.save(savedTransactionEntity);

        ValidateChallengeRequest validateChallengeRequest = new ValidateChallengeRequest();
        validateChallengeRequest.setExternalId(savedTransactionEntity.getExternalId());

        try {
            customer2faService.validateChallenge(userId, validateChallengeRequest);
            Assert.fail("Debe tirar tenpo exception");
        } catch (TenpoException te) {
            Assert.assertEquals("Debe ser 422", HttpStatus.UNPROCESSABLE_ENTITY, te.getCode());
            Assert.assertEquals("Debe ser 1202", ErrorCode.TRANSACTION_CONTEXT_CLOSED, te.getErrorCode());
            throw te;
        } catch (Exception e) {
            Assert.fail("Debe tirar tenpo exception");
        }
    }

    @Test(expected = TenpoException.class)
    public void whenTrxCanceled_ThenThrowException() {
        UUID userId = UUID.randomUUID();
        // Prepare user response OK
        UserResponse userResponse = new UserResponse();
        userResponse.setId(userId);
        userResponse.setState(UserStateType.ACTIVE);
        when(userRestClient.getUser(Mockito.any(UUID.class)))
                .thenReturn(Optional.of(userResponse));

        // Agrega la transaccion ya existente
        CustomerTransactionContextEntity savedTransactionEntity = createTransaction(userId);
        savedTransactionEntity.setStatus(CustomerTransactionStatus.CANCEL);
        customerTransactionContextRespository.save(savedTransactionEntity);

        ValidateChallengeRequest validateChallengeRequest = new ValidateChallengeRequest();
        validateChallengeRequest.setExternalId(savedTransactionEntity.getExternalId());

        try {
            customer2faService.validateChallenge(userId, validateChallengeRequest);
            Assert.fail("Debe tirar tenpo exception");
        } catch (TenpoException te) {
            Assert.assertEquals("Debe ser 422", HttpStatus.UNPROCESSABLE_ENTITY, te.getCode());
            Assert.assertEquals("Debe ser 1201", ErrorCode.TRANSACTION_CONTEXT_CANCELED, te.getErrorCode());
            throw te;
        } catch (Exception e) {
            Assert.fail("Debe tirar tenpo exception");
        }
    }

    @Test(expected = TenpoException.class)
    public void whenTrxExpiredByStatus_ThenThrowException() {
        UUID userId = UUID.randomUUID();
        // Prepare user response OK
        UserResponse userResponse = new UserResponse();
        userResponse.setId(userId);
        userResponse.setState(UserStateType.ACTIVE);
        when(userRestClient.getUser(Mockito.any(UUID.class)))
                .thenReturn(Optional.of(userResponse));

        // Agrega la transaccion ya existente
        CustomerTransactionContextEntity savedTransactionEntity = createTransaction(userId);
        savedTransactionEntity.setStatus(CustomerTransactionStatus.EXPIRED);
        customerTransactionContextRespository.save(savedTransactionEntity);

        ValidateChallengeRequest validateChallengeRequest = new ValidateChallengeRequest();
        validateChallengeRequest.setExternalId(savedTransactionEntity.getExternalId());

        try {
            customer2faService.validateChallenge(userId, validateChallengeRequest);
            Assert.fail("Debe tirar tenpo exception");
        } catch (TenpoException te) {
            Assert.assertEquals("Debe ser 422", HttpStatus.UNPROCESSABLE_ENTITY, te.getCode());
            Assert.assertEquals("Debe ser 1200", ErrorCode.TRANSACTION_CONTEXT_EXPIRED, te.getErrorCode());
            throw te;
        } catch (Exception e) {
            Assert.fail("Debe tirar tenpo exception");
        }
    }

    @Test(expected = TenpoException.class)
    public void whenTrxExpiredByTime_ThenThrowException() {
        UUID userId = UUID.randomUUID();
        // Prepare user response OK
        UserResponse userResponse = new UserResponse();
        userResponse.setId(userId);
        userResponse.setState(UserStateType.ACTIVE);
        when(userRestClient.getUser(Mockito.any(UUID.class)))
                .thenReturn(Optional.of(userResponse));

        // Agrega la transaccion ya existente
        CustomerTransactionContextEntity savedTransactionEntity = createTransaction(userId);
        savedTransactionEntity.setStatus(CustomerTransactionStatus.PENDING);
        savedTransactionEntity.setCreated(LocalDateTime.now(ZoneId.of("UTC")).minusMinutes(transactionContextProperties.getExpirationTimeInMinutes()));
        customerTransactionContextRespository.save(savedTransactionEntity);

        ValidateChallengeRequest validateChallengeRequest = new ValidateChallengeRequest();
        validateChallengeRequest.setExternalId(savedTransactionEntity.getExternalId());

        try {
            customer2faService.validateChallenge(userId, validateChallengeRequest);
            Assert.fail("Debe tirar tenpo exception");
        } catch (TenpoException te) {
            Assert.assertEquals("Debe ser 422", HttpStatus.UNPROCESSABLE_ENTITY, te.getCode());
            Assert.assertEquals("Debe ser 1200", ErrorCode.TRANSACTION_CONTEXT_EXPIRED, te.getErrorCode());
            throw te;
        } catch (Exception e) {
            Assert.fail("Debe tirar tenpo exception");
        }

        // Revisar que haya cambiado en la BD
        Optional<CustomerTransactionContextEntity> storedTransaction = customerTransactionContextRespository.findByExternalId(savedTransactionEntity.getExternalId());
        Assert.assertTrue("Debe existir", storedTransaction.isPresent());
        Assert.assertEquals("Ahora debe tener estado expirado", CustomerTransactionStatus.EXPIRED, storedTransaction.get().getStatus());
    }

    @Test(expected = TenpoException.class)
    public void whenTrxRejectedByStatus_ThenThrowException() {
        UUID userId = UUID.randomUUID();
        // Prepare user response OK
        UserResponse userResponse = new UserResponse();
        userResponse.setId(userId);
        userResponse.setState(UserStateType.ACTIVE);
        when(userRestClient.getUser(Mockito.any(UUID.class)))
                .thenReturn(Optional.of(userResponse));

        // Agrega la transaccion ya existente
        CustomerTransactionContextEntity savedTransactionEntity = createTransaction(userId);
        savedTransactionEntity.setStatus(CustomerTransactionStatus.REJECTED);
        customerTransactionContextRespository.save(savedTransactionEntity);

        ValidateChallengeRequest validateChallengeRequest = new ValidateChallengeRequest();
        validateChallengeRequest.setExternalId(savedTransactionEntity.getExternalId());

        try {
            customer2faService.validateChallenge(userId, validateChallengeRequest);
            Assert.fail("Debe tirar tenpo exception");
        } catch (TenpoException te) {
            Assert.assertEquals("Debe ser 422", HttpStatus.UNPROCESSABLE_ENTITY, te.getCode());
            Assert.assertEquals("Debe ser 1151", ErrorCode.BLOCKED_PASSWORD, te.getErrorCode());
            throw te;
        } catch (Exception e) {
            Assert.fail("Debe tirar tenpo exception");
        }
    }

    @Test(expected = TenpoException.class)
    public void whenTrxRejectedByAttempts_ThenThrowException() throws JsonProcessingException {
        UUID userId = UUID.randomUUID();
        // Prepare user response OK
        UserResponse userResponse = new UserResponse();
        userResponse.setId(userId);
        userResponse.setState(UserStateType.ACTIVE);
        when(userRestClient.getUser(Mockito.any(UUID.class)))
                .thenReturn(Optional.of(userResponse));

        doNothing().when(eventProducerService)
                .sendLockEvent(Mockito.any(LockUnlockUserDto.class));

        // Agrega la transaccion ya existente
        CustomerTransactionContextEntity savedTransactionEntity = createTransaction(userId);
        savedTransactionEntity.setStatus(CustomerTransactionStatus.PENDING);
        savedTransactionEntity.setAttempts(transactionContextProperties.getPasswordAttempts() + 1);
        customerTransactionContextRespository.save(savedTransactionEntity);

        ValidateChallengeRequest validateChallengeRequest = new ValidateChallengeRequest();
        validateChallengeRequest.setExternalId(savedTransactionEntity.getExternalId());

        try {
            customer2faService.validateChallenge(userId, validateChallengeRequest);
            Assert.fail("Debe tirar tenpo exception");
        } catch (TenpoException te) {
            Assert.assertEquals("Debe ser 422", HttpStatus.UNPROCESSABLE_ENTITY, te.getCode());
            Assert.assertEquals("Debe ser 1151", ErrorCode.BLOCKED_PASSWORD, te.getErrorCode());
            throw te;
        } catch (Exception e) {
            Assert.fail("Debe tirar tenpo exception");
        }

        // Revisar que haya cambiado en la BD
        Optional<CustomerTransactionContextEntity> storedTransaction = customerTransactionContextRespository.findByExternalId(savedTransactionEntity.getExternalId());
        Assert.assertTrue("Debe existir", storedTransaction.isPresent());
        Assert.assertEquals("Ahora debe tener estado rejected", CustomerTransactionStatus.REJECTED, storedTransaction.get().getStatus());
    }

    @Test
    public void whenVerifierRestClientAnswersTrue_ThenChangeStatus() throws JsonProcessingException {
        UUID userId = UUID.randomUUID();

        // Agrega la transaccion ya existente
        CustomerTransactionContextEntity savedTransactionEntity = createTransaction(userId);
        customerTransactionContextRespository.save(savedTransactionEntity);

        // Prepare user response OK
        UserResponse userResponse = new UserResponse();
        userResponse.setId(userId);
        userResponse.setState(UserStateType.ACTIVE);
        when(userRestClient.getUser(Mockito.any(UUID.class)))
                .thenReturn(Optional.of(userResponse));

        // Verifier responde OK
        when(verifierRestClient.validateTwoFactorCode(any(), any()))
                .thenReturn(true);

        ValidateChallengeRequest validateChallengeRequest = new ValidateChallengeRequest();
        validateChallengeRequest.setExternalId(savedTransactionEntity.getExternalId());
        validateChallengeRequest.setResponse("123456");

        ValidateChallengeResponse validationResponse = customer2faService.validateChallenge(userId, validateChallengeRequest);
        Assert.assertEquals("Debe retornar el external id", savedTransactionEntity.getExternalId(), validationResponse.getExternalId());
        Assert.assertEquals("Debe retornar EXITO", ChallengeResult.AUTH_EXITOSA, validationResponse.getResult());

        // Revisar que haya cambiado en la BD
        Optional<CustomerTransactionContextEntity> storedTransaction = customerTransactionContextRespository.findByExternalId(savedTransactionEntity.getExternalId());
        Assert.assertTrue("Debe existir", storedTransaction.isPresent());
        Assert.assertEquals("Ahora debe tener estado AUTHORIZED", CustomerTransactionStatus.AUTHORIZED, storedTransaction.get().getStatus());
    }

    @Test
    public void whenVerifierRestClientAnswersFalse_ThenChangeStatus() throws JsonProcessingException {
        UUID userId = UUID.randomUUID();

        // Agrega la transaccion ya existente
        CustomerTransactionContextEntity savedTransactionEntity = createTransaction(userId);
        customerTransactionContextRespository.save(savedTransactionEntity);

        // Prepare user response OK
        UserResponse userResponse = new UserResponse();
        userResponse.setId(userId);
        userResponse.setState(UserStateType.ACTIVE);
        when(userRestClient.getUser(Mockito.any(UUID.class)))
                .thenReturn(Optional.of(userResponse));

        // Verifier responde FALSE
        when(verifierRestClient.validateTwoFactorCode(Mockito.any(UUID.class), any()))
                .thenReturn(false);

        ValidateChallengeRequest validateChallengeRequest = new ValidateChallengeRequest();
        validateChallengeRequest.setExternalId(savedTransactionEntity.getExternalId());
        validateChallengeRequest.setResponse("123456");

        ValidateChallengeResponse validationResponse = customer2faService.validateChallenge(userId, validateChallengeRequest);
        Assert.assertEquals("Debe retornar el external id", savedTransactionEntity.getExternalId(), validationResponse.getExternalId());
        Assert.assertEquals("Debe retornar FALLO", ChallengeResult.AUTH_FALLIDA, validationResponse.getResult());

        // Revisar que haya cambiado en la BD
        Optional<CustomerTransactionContextEntity> storedTransaction = customerTransactionContextRespository.findByExternalId(savedTransactionEntity.getExternalId());
        Assert.assertTrue("Debe existir", storedTransaction.isPresent());
        Assert.assertEquals("Debe seguir con estado PENDING", CustomerTransactionStatus.PENDING, storedTransaction.get().getStatus());
        Assert.assertEquals("Debe tener marcado un intento mas", Integer.valueOf(1), storedTransaction.get().getAttempts());
    }

    private CustomerTransactionContextEntity createTransaction(UUID userId) {
        CustomerTransactionContextEntity customerTransactionContextEntity = new CustomerTransactionContextEntity();
        customerTransactionContextEntity.setId(UUID.randomUUID());
        customerTransactionContextEntity.setExternalId(UUID.randomUUID().toString());
        customerTransactionContextEntity.setUserId(userId);
        customerTransactionContextEntity.setStatus(CustomerTransactionStatus.PENDING);
        customerTransactionContextEntity.setAttempts(0);
        customerTransactionContextEntity.setCreated(LocalDateTime.now(ZoneId.of("UTC")));
        return customerTransactionContextEntity;
    }
}
