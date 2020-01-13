package cl.tenpo.customerauthentication.unit.service;

import cl.tenpo.customerauthentication.CustomerAuthenticationMsApplicationTests;
import cl.tenpo.customerauthentication.api.dto.ValidateChallengeRequest;
import cl.tenpo.customerauthentication.api.dto.ValidateChallengeResponse;
import cl.tenpo.customerauthentication.constants.ErrorCode;
import cl.tenpo.customerauthentication.dto.CustomerTransactionContextDTO;
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
import org.junit.Assert;
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

import static cl.tenpo.customerauthentication.constants.ErrorCode.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@SpringBootTest
public class Customer2faServiceValidateChallengeTests extends CustomerAuthenticationMsApplicationTests {

    @Autowired
    private Customer2faService customer2faService;

    @MockBean
    private CustomerChallengeService customerChallengeService;

    @MockBean
    private UserRestClient userRestClient;

    @MockBean
    private VerifierRestClient verifierRestClient;

    @Autowired
    private CustomerTransactionContextProperties transactionContextProperties;

    @Test(expected = TenpoException.class)
    public void validateChallengeExternalNotFound(){
        when(customerChallengeService.findByExternalId(anyString()))
                .thenReturn(Optional.empty());
        try{
            ValidateChallengeRequest validateChallengeRequest = new ValidateChallengeRequest();
            customer2faService.validateChallenge(UUID.randomUUID(), validateChallengeRequest);
            Assert.fail("Can't be here");
        }catch (TenpoException e){
            Assert.assertEquals("HttpStatus debe ser igual", HttpStatus.NOT_FOUND, e.getCode());
            Assert.assertEquals("Codigo debe ser igual", EXTERNAL_ID_NOT_FOUND, e.getErrorCode());
            throw e;
        }
    }

    @Test(expected = TenpoException.class)
    public void validateChallengeTrxContextAlreadyAuthorized() {

        CustomerTransactionContextDTO customerTransactionContextDTO = createTransaction();
        customerTransactionContextDTO.setStatus(CustomerTransactionStatus.AUTHORIZED);
        when(customerChallengeService.findByExternalId(anyString()))
                .thenReturn(Optional.of(customerTransactionContextDTO));

        try {
            ValidateChallengeRequest validateChallengeRequest = new ValidateChallengeRequest();
            validateChallengeRequest.setExternalId(UUID.randomUUID().toString());
            customer2faService.validateChallenge(UUID.randomUUID(), validateChallengeRequest);
            Assert.fail("Can't be here");
        } catch (TenpoException e) {
            Assert.assertEquals("HttpStatus debe ser igual", HttpStatus.UNPROCESSABLE_ENTITY, e.getCode());
            Assert.assertEquals("Codigo debe ser 1202", TRANSACTION_CONTEXT_CLOSED, e.getErrorCode());
            throw e;
        }
    }

    @Test(expected = TenpoException.class)
    public void validateChallengeTrxContextAlreadyCanceled() {

        CustomerTransactionContextDTO customerTransactionContextDTO = createTransaction();
        customerTransactionContextDTO.setStatus(CustomerTransactionStatus.CANCEL);
        when(customerChallengeService.findByExternalId(anyString()))
                .thenReturn(Optional.of(customerTransactionContextDTO));

        try {
            ValidateChallengeRequest validateChallengeRequest = new ValidateChallengeRequest();
            validateChallengeRequest.setExternalId(UUID.randomUUID().toString());
            customer2faService.validateChallenge(UUID.randomUUID(), validateChallengeRequest);
            Assert.fail("Can't be here");
        } catch (TenpoException e) {
            Assert.assertEquals("HttpStatus debe ser igual", HttpStatus.UNPROCESSABLE_ENTITY, e.getCode());
            Assert.assertEquals("Codigo debe ser 1201", TRANSACTION_CONTEXT_CANCELED, e.getErrorCode());
            throw e;
        }
    }

    @Test(expected = TenpoException.class)
    public void validateChallengeTrxContextAlreadyExpiredByStatus() {

        CustomerTransactionContextDTO customerTransactionContextDTO = createTransaction();
        customerTransactionContextDTO.setStatus(CustomerTransactionStatus.EXPIRED);
        when(customerChallengeService.findByExternalId(anyString()))
                .thenReturn(Optional.of(customerTransactionContextDTO));

        try {
            ValidateChallengeRequest validateChallengeRequest = new ValidateChallengeRequest();
            validateChallengeRequest.setExternalId(UUID.randomUUID().toString());
            customer2faService.validateChallenge(UUID.randomUUID(), validateChallengeRequest);
            Assert.fail("Can't be here");
        } catch (TenpoException e) {
            Assert.assertEquals("HttpStatus debe ser igual", HttpStatus.UNPROCESSABLE_ENTITY, e.getCode());
            Assert.assertEquals("Codigo debe ser 1201", TRANSACTION_CONTEXT_EXPIRED, e.getErrorCode());
            throw e;
        }
    }

    @Test(expected = TenpoException.class)
    public void validateChallengeTrxContextAlreadyExpiredByTime() {

        CustomerTransactionContextDTO customerTransactionContextDTO = createTransaction();
        customerTransactionContextDTO.setCreated(LocalDateTime.now(ZoneId.of("UTC")).minusMinutes(transactionContextProperties.getExpirationTimeInMinutes() + 1));
        when(customerChallengeService.findByExternalId(anyString()))
                .thenReturn(Optional.of(customerTransactionContextDTO));

        try {
            ValidateChallengeRequest validateChallengeRequest = new ValidateChallengeRequest();
            validateChallengeRequest.setExternalId(UUID.randomUUID().toString());
            customer2faService.validateChallenge(UUID.randomUUID(), validateChallengeRequest);
            Assert.fail("Can't be here");
        } catch (TenpoException e) {
            Assert.assertEquals("HttpStatus debe ser igual", HttpStatus.UNPROCESSABLE_ENTITY, e.getCode());
            Assert.assertEquals("Codigo debe ser 1201", TRANSACTION_CONTEXT_EXPIRED, e.getErrorCode());
            throw e;
        }
    }

    @Test(expected = TenpoException.class)
    public void validateChallengeTrxContextRejectedByStatus() {

        CustomerTransactionContextDTO customerTransactionContextDTO = createTransaction();
        customerTransactionContextDTO.setStatus(CustomerTransactionStatus.REJECTED);
        when(customerChallengeService.findByExternalId(anyString()))
                .thenReturn(Optional.of(customerTransactionContextDTO));

        try {
            ValidateChallengeRequest validateChallengeRequest = new ValidateChallengeRequest();
            validateChallengeRequest.setExternalId(UUID.randomUUID().toString());
            customer2faService.validateChallenge(UUID.randomUUID(), validateChallengeRequest);
            Assert.fail("Can't be here");
        } catch (TenpoException e) {
            Assert.assertEquals("HttpStatus debe ser igual", HttpStatus.UNPROCESSABLE_ENTITY, e.getCode());
            Assert.assertEquals("Codigo debe ser igual 1151", BLOCKED_PASSWORD, e.getErrorCode());
            throw e;
        }
    }

    @Test(expected = TenpoException.class)
    public void validateChallengeTrxContextRejectedByAttempts() {

        CustomerTransactionContextDTO customerTransactionContextDTO = createTransaction();
        customerTransactionContextDTO.setAttempts(transactionContextProperties.getPasswordAttempts() + 1);
        when(customerChallengeService.findByExternalId(anyString()))
                .thenReturn(Optional.of(customerTransactionContextDTO));

        try {
            ValidateChallengeRequest validateChallengeRequest = new ValidateChallengeRequest();
            validateChallengeRequest.setExternalId(UUID.randomUUID().toString());
            customer2faService.validateChallenge(UUID.randomUUID(), validateChallengeRequest);
            Assert.fail("Can't be here");
        } catch (TenpoException e) {
            Assert.assertEquals("HttpStatus debe ser igual", HttpStatus.UNPROCESSABLE_ENTITY, e.getCode());
            Assert.assertEquals("Codigo debe ser igual 1151", BLOCKED_PASSWORD, e.getErrorCode());
            throw e;
        }
    }

    @Test(expected = TenpoException.class)
    public void validateChallengeUserRestClientException() {

        CustomerTransactionContextDTO customerTransactionContextDTO = createTransaction();
        when(customerChallengeService.findByExternalId(anyString()))
                .thenReturn(Optional.of(customerTransactionContextDTO));

        when(userRestClient.getUser(any(UUID.class)))
                .thenThrow(new NullPointerException());

        try {
            ValidateChallengeRequest validateChallengeRequest = new ValidateChallengeRequest();
            validateChallengeRequest.setExternalId(UUID.randomUUID().toString());
            customer2faService.validateChallenge(UUID.randomUUID(), validateChallengeRequest);
            Assert.fail("Can't be here");
        } catch (TenpoException e) {
            Assert.assertEquals("HttpStatus debe ser 401", HttpStatus.UNAUTHORIZED, e.getCode());
            Assert.assertEquals("Codigo debe ser 1412", INVALID_TOKEN, e.getErrorCode());
            throw e;
        }
    }

    @Test(expected = TenpoException.class)
    public void validateChallengeUserNotFound() {

        CustomerTransactionContextDTO customerTransactionContextDTO = createTransaction();
        when(customerChallengeService.findByExternalId(anyString()))
                .thenReturn(Optional.of(customerTransactionContextDTO));

        when(userRestClient.getUser(any(UUID.class)))
                .thenReturn(Optional.empty());

        try {
            ValidateChallengeRequest validateChallengeRequest = new ValidateChallengeRequest();
            validateChallengeRequest.setExternalId(UUID.randomUUID().toString());
            customer2faService.validateChallenge(UUID.randomUUID(), validateChallengeRequest);
            Assert.fail("Can't be here");
        } catch (TenpoException e) {
            Assert.assertEquals("HttpStatus debe ser 401", HttpStatus.UNAUTHORIZED, e.getCode());
            Assert.assertEquals("Codigo debe ser 1412", INVALID_TOKEN, e.getErrorCode());
            throw e;
        }
    }

    @Test(expected = TenpoException.class)
    public void validateChallengeUserNoActive() {

        CustomerTransactionContextDTO customerTransactionContextDTO = createTransaction();
        when(customerChallengeService.findByExternalId(anyString()))
                .thenReturn(Optional.of(customerTransactionContextDTO));

        UserResponse userResponse = new UserResponse();
        userResponse.setId(UUID.randomUUID());
        userResponse.setState(UserStateType.BLOCKED);

        when(userRestClient.getUser(any(UUID.class)))
                .thenReturn(Optional.of(userResponse));

        try {
            ValidateChallengeRequest validateChallengeRequest = new ValidateChallengeRequest();
            validateChallengeRequest.setExternalId(UUID.randomUUID().toString());
            customer2faService.validateChallenge(UUID.randomUUID(), validateChallengeRequest);
            Assert.fail("Can't be here");
        } catch (TenpoException e) {
            Assert.assertEquals("HttpStatus debe ser 401", HttpStatus.UNAUTHORIZED, e.getCode());
            Assert.assertEquals("Codigo debe ser 1412", INVALID_TOKEN, e.getErrorCode());
            throw e;
        }
    }

    @Test
    public void validateChallengeVerifierRestThrowsException() {

        CustomerTransactionContextDTO customerTransactionContextDTO = createTransaction();
        when(customerChallengeService.findByExternalId(anyString()))
                .thenReturn(Optional.of(customerTransactionContextDTO));

        UserResponse userResponse = new UserResponse();
        userResponse.setId(UUID.randomUUID());
        userResponse.setState(UserStateType.ACTIVE);

        when(userRestClient.getUser(any(UUID.class)))
                .thenReturn(Optional.of(userResponse));

        when(verifierRestClient.validateTwoFactorCode(any(UUID.class), any()))
                .thenThrow(new TenpoException(HttpStatus.INTERNAL_SERVER_ERROR, INTERNAL_ERROR));
        try {
            ValidateChallengeRequest validateChallengeRequest = new ValidateChallengeRequest();
            validateChallengeRequest.setExternalId(customerTransactionContextDTO.getExternalId());
            validateChallengeRequest.setResponse("123456");

            customer2faService.validateChallenge(UUID.randomUUID(), validateChallengeRequest);
            Assert.fail("Debe tirar excepcion");
        } catch (TenpoException e) {
            Assert.assertEquals("Debe ser la misma excepcion", HttpStatus.INTERNAL_SERVER_ERROR, e.getCode());
            Assert.assertEquals("Debe ser la misma excepcion", INTERNAL_ERROR, e.getErrorCode());
        }
    }

    @Test
    public void validateChallengeOK() {

        CustomerTransactionContextDTO customerTransactionContextDTO = createTransaction();
        when(customerChallengeService.findByExternalId(anyString()))
                .thenReturn(Optional.of(customerTransactionContextDTO));

        UserResponse userResponse = new UserResponse();
        userResponse.setId(UUID.randomUUID());
        userResponse.setState(UserStateType.ACTIVE);

        when(userRestClient.getUser(any(UUID.class)))
                .thenReturn(Optional.of(userResponse));

        when(verifierRestClient.validateTwoFactorCode(any(UUID.class), any()))
                .thenReturn(true);
        try {
            ValidateChallengeRequest validateChallengeRequest = new ValidateChallengeRequest();
            validateChallengeRequest.setExternalId(customerTransactionContextDTO.getExternalId());
            validateChallengeRequest.setResponse("123456");

            customer2faService.validateChallenge(UUID.randomUUID(), validateChallengeRequest);
            ValidateChallengeResponse response=customer2faService.validateChallenge(UUID.randomUUID(), validateChallengeRequest);
            Assert.assertNotNull("Debe existir un response",response);
            Assert.assertEquals("Debe tener ", ChallengeResult.AUTH_EXITOSA,response.getResult());
            Assert.assertEquals("External id deben ser iguales",validateChallengeRequest.getExternalId(),response.getExternalId());
        }catch (TenpoException e){
            Assert.fail("Can't be here");
        }
    }

    @Test
    public void validateChallengeFalse() {

        CustomerTransactionContextDTO customerTransactionContextDTO = createTransaction();
        when(customerChallengeService.findByExternalId(anyString()))
                .thenReturn(Optional.of(customerTransactionContextDTO));

        when(customerChallengeService.addTransactionContextAttempt(any()))
                .thenReturn(Optional.of(customerTransactionContextDTO));

        UserResponse userResponse = new UserResponse();
        userResponse.setId(UUID.randomUUID());
        userResponse.setState(UserStateType.ACTIVE);

        when(userRestClient.getUser(any(UUID.class)))
                .thenReturn(Optional.of(userResponse));

        when(verifierRestClient.validateTwoFactorCode(any(UUID.class),Mockito.anyString()))
                .thenReturn(false);
        try {
            ValidateChallengeRequest validateChallengeRequest = new ValidateChallengeRequest();
            validateChallengeRequest.setExternalId(customerTransactionContextDTO.getExternalId());
            ValidateChallengeResponse response = customer2faService.validateChallenge(UUID.randomUUID(), validateChallengeRequest);
            Assert.assertNotNull("Debe existir un response", response);
            Assert.assertEquals("Debe tener ", ChallengeResult.AUTH_FALLIDA, response.getResult());
            Assert.assertEquals("External id deben ser iguales",validateChallengeRequest.getExternalId(),response.getExternalId());
        }catch (TenpoException e){
            Assert.fail("Can't be here");
        }
    }

    @Test (expected = TenpoException.class)
    public void validateChallengeFalseThrowsBlockedPasswordException() {

        CustomerTransactionContextDTO customerTransactionContextDTO = createTransaction();
        when(customerChallengeService.findByExternalId(anyString()))
                .thenReturn(Optional.of(customerTransactionContextDTO));

        CustomerTransactionContextDTO attemptedTransaction = createTransaction();
        attemptedTransaction.setAttempts(transactionContextProperties.getPasswordAttempts()); // Este es su ultimo intento
        when(customerChallengeService.addTransactionContextAttempt(any()))
                .thenReturn(Optional.of(attemptedTransaction));

        UserResponse userResponse = new UserResponse();
        userResponse.setId(UUID.randomUUID());
        userResponse.setState(UserStateType.ACTIVE);

        when(userRestClient.getUser(any(UUID.class)))
                .thenReturn(Optional.of(userResponse));

        when(verifierRestClient.validateTwoFactorCode(any(UUID.class), Mockito.anyString()))
                .thenReturn(false);
        try {
            ValidateChallengeRequest validateChallengeRequest = new ValidateChallengeRequest();
            validateChallengeRequest.setExternalId(customerTransactionContextDTO.getExternalId());
            customer2faService.validateChallenge(UUID.randomUUID(), validateChallengeRequest);
            Assert.fail("Can't be here");
        } catch (TenpoException e) {
            Assert.assertEquals("HttpStatus debe ser igual", HttpStatus.UNPROCESSABLE_ENTITY, e.getCode());
            Assert.assertEquals("Codigo debe ser igual 1151", BLOCKED_PASSWORD, e.getErrorCode());
            throw e;
        }
    }

    private CustomerTransactionContextDTO createTransaction() {
        CustomerTransactionContextDTO customerTransactionContextDTO = new CustomerTransactionContextDTO();
        customerTransactionContextDTO.setExternalId(UUID.randomUUID().toString());
        customerTransactionContextDTO.setStatus(CustomerTransactionStatus.PENDING);
        customerTransactionContextDTO.setAttempts(0);
        customerTransactionContextDTO.setCreated(LocalDateTime.now(ZoneId.of("UTC")));
        return customerTransactionContextDTO;
    }
}
