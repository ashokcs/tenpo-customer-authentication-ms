package cl.tenpo.customerauthentication.integration.service;

import cl.tenpo.customerauthentication.CustomerAuthenticationMsApplicationTests;
import cl.tenpo.customerauthentication.api.dto.AbortChallengeRequest;
import cl.tenpo.customerauthentication.api.dto.AbortChallengeResponse;
import cl.tenpo.customerauthentication.api.dto.ValidateChallengeRequest;
import cl.tenpo.customerauthentication.api.dto.ValidateChallengeResponse;
import cl.tenpo.customerauthentication.constants.ErrorCode;
import cl.tenpo.customerauthentication.database.entity.CustomerTransactionContextEntity;
import cl.tenpo.customerauthentication.database.repository.CustomerChallengeRepository;
import cl.tenpo.customerauthentication.database.repository.CustomerTransactionContextRespository;
import cl.tenpo.customerauthentication.exception.TenpoException;
import cl.tenpo.customerauthentication.externalservice.user.UserRestClient;
import cl.tenpo.customerauthentication.externalservice.user.dto.UserResponse;
import cl.tenpo.customerauthentication.externalservice.user.dto.UserStateType;
import cl.tenpo.customerauthentication.externalservice.verifier.VerifierRestClient;
import cl.tenpo.customerauthentication.model.ChallengeResult;
import cl.tenpo.customerauthentication.model.CustomerTransactionStatus;
import cl.tenpo.customerauthentication.properties.CustomerTransactionContextProperties;
import cl.tenpo.customerauthentication.service.Customer2faService;
import cl.tenpo.customerauthentication.service.CustomerChallengeService;
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

import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@SpringBootTest
public class Customer2faServiceAbortChallengeTests extends CustomerAuthenticationMsApplicationTests {

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

    @Before
    @After
    public void clearDB() {
        customerChallengeRepository.deleteAll();
        customerTransactionContextRespository.deleteAll();
    }

    @Test(expected = TenpoException.class)
    public void whenTrxNotFound_ThenThrowNotFoundException() {
        AbortChallengeRequest abortChallengeRequest = new AbortChallengeRequest();
        abortChallengeRequest.setExternalId(UUID.randomUUID());

        try {
            customer2faService.abortChallenge(UUID.randomUUID(), abortChallengeRequest);
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

        // Agrega la transaccion ya existente
        CustomerTransactionContextEntity savedTransactionEntity = createTransaction(userId);
        savedTransactionEntity.setStatus(CustomerTransactionStatus.AUTHORIZED);
        customerTransactionContextRespository.save(savedTransactionEntity);

        AbortChallengeRequest abortChallengeRequest = new AbortChallengeRequest();
        abortChallengeRequest.setExternalId(savedTransactionEntity.getExternalId());

        try {
            customer2faService.abortChallenge(userId, abortChallengeRequest);
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
    public void whenTrxExpiredByStatus_ThenThrowException() {
        UUID userId = UUID.randomUUID();

        // Agrega la transaccion ya existente
        CustomerTransactionContextEntity savedTransactionEntity = createTransaction(userId);
        savedTransactionEntity.setStatus(CustomerTransactionStatus.EXPIRED);
        customerTransactionContextRespository.save(savedTransactionEntity);

        AbortChallengeRequest abortChallengeRequest = new AbortChallengeRequest();
        abortChallengeRequest.setExternalId(savedTransactionEntity.getExternalId());

        try {
            customer2faService.abortChallenge(userId, abortChallengeRequest);
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

        // Agrega la transaccion ya existente
        CustomerTransactionContextEntity savedTransactionEntity = createTransaction(userId);
        savedTransactionEntity.setStatus(CustomerTransactionStatus.PENDING);
        savedTransactionEntity.setCreated(LocalDateTime.now(ZoneId.of("UTC")).minusMinutes(transactionContextProperties.getExpirationTimeInMinutes()));
        customerTransactionContextRespository.save(savedTransactionEntity);

        AbortChallengeRequest abortChallengeRequest = new AbortChallengeRequest();
        abortChallengeRequest.setExternalId(savedTransactionEntity.getExternalId());

        try {
            customer2faService.abortChallenge(userId, abortChallengeRequest);
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

        // Agrega la transaccion ya existente
        CustomerTransactionContextEntity savedTransactionEntity = createTransaction(userId);
        savedTransactionEntity.setStatus(CustomerTransactionStatus.REJECTED);
        customerTransactionContextRespository.save(savedTransactionEntity);

        AbortChallengeRequest abortChallengeRequest = new AbortChallengeRequest();
        abortChallengeRequest.setExternalId(savedTransactionEntity.getExternalId());

        try {
            customer2faService.abortChallenge(userId, abortChallengeRequest);
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
    public void whenTrxRejectedByAttempts_ThenThrowException() {
        UUID userId = UUID.randomUUID();

        // Agrega la transaccion ya existente
        CustomerTransactionContextEntity savedTransactionEntity = createTransaction(userId);
        savedTransactionEntity.setStatus(CustomerTransactionStatus.PENDING);
        savedTransactionEntity.setAttempts(transactionContextProperties.getPasswordAttempts() + 1);
        customerTransactionContextRespository.save(savedTransactionEntity);

        AbortChallengeRequest abortChallengeRequest = new AbortChallengeRequest();
        abortChallengeRequest.setExternalId(savedTransactionEntity.getExternalId());

        try {
            customer2faService.abortChallenge(userId, abortChallengeRequest);
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
    public void whenVerifierRestClientAnswersTrue_ThenChangeStatus() {
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
        when(verifierRestClient.validateTwoFactorCode(Mockito.any(UUID.class), Mockito.anyString()))
                .thenReturn(true);

        AbortChallengeRequest abortChallengeRequest = new AbortChallengeRequest();
        abortChallengeRequest.setExternalId(savedTransactionEntity.getExternalId());

        AbortChallengeResponse abortChallengeResponse = customer2faService.abortChallenge(userId, abortChallengeRequest);
        Assert.assertEquals("Debe retornar el external id", savedTransactionEntity.getExternalId(), abortChallengeResponse.getExternalId());
        Assert.assertEquals("Debe retornar CANCELADO", ChallengeResult.CANCELADO, abortChallengeResponse.getResult());

        // Revisar que haya cambiado en la BD
        Optional<CustomerTransactionContextEntity> storedTransaction = customerTransactionContextRespository.findByExternalId(savedTransactionEntity.getExternalId());
        Assert.assertTrue("Debe existir", storedTransaction.isPresent());
        Assert.assertEquals("Ahora debe tener estado CANCEL", CustomerTransactionStatus.CANCEL, storedTransaction.get().getStatus());
    }

    @Test
    public void whenVerifierRestClientAnswersTrueAndStatusCancel_ThenKeepStatus() {
        UUID userId = UUID.randomUUID();

        // Agrega la transaccion ya existente
        CustomerTransactionContextEntity savedTransactionEntity = createTransaction(userId);
        savedTransactionEntity.setStatus(CustomerTransactionStatus.CANCEL);
        customerTransactionContextRespository.save(savedTransactionEntity);

        // Prepare user response OK
        UserResponse userResponse = new UserResponse();
        userResponse.setId(userId);
        userResponse.setState(UserStateType.ACTIVE);
        when(userRestClient.getUser(Mockito.any(UUID.class)))
                .thenReturn(Optional.of(userResponse));

        // Verifier responde OK
        when(verifierRestClient.validateTwoFactorCode(Mockito.any(UUID.class), Mockito.anyString()))
                .thenReturn(true);

        AbortChallengeRequest abortChallengeRequest = new AbortChallengeRequest();
        abortChallengeRequest.setExternalId(savedTransactionEntity.getExternalId());

        AbortChallengeResponse abortChallengeResponse = customer2faService.abortChallenge(userId, abortChallengeRequest);
        Assert.assertEquals("Debe retornar el external id", savedTransactionEntity.getExternalId(), abortChallengeResponse.getExternalId());
        Assert.assertEquals("Debe retornar CANCELADO", ChallengeResult.CANCELADO, abortChallengeResponse.getResult());

        // Revisar que haya cambiado en la BD
        Optional<CustomerTransactionContextEntity> storedTransaction = customerTransactionContextRespository.findByExternalId(savedTransactionEntity.getExternalId());
        Assert.assertTrue("Debe existir", storedTransaction.isPresent());
        Assert.assertEquals("Debe mantener estado CANCEL", CustomerTransactionStatus.CANCEL, storedTransaction.get().getStatus());
    }

    private CustomerTransactionContextEntity createTransaction(UUID userId) {
        CustomerTransactionContextEntity customerTransactionContextEntity = new CustomerTransactionContextEntity();
        customerTransactionContextEntity.setId(UUID.randomUUID());
        customerTransactionContextEntity.setExternalId(UUID.randomUUID());
        customerTransactionContextEntity.setUserId(userId);
        customerTransactionContextEntity.setStatus(CustomerTransactionStatus.PENDING);
        customerTransactionContextEntity.setAttempts(0);
        customerTransactionContextEntity.setCreated(LocalDateTime.now(ZoneId.of("UTC")));
        return customerTransactionContextEntity;
    }
}
