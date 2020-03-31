package cl.tenpo.customerauthentication.unit.service;


import cl.tenpo.customerauthentication.CustomerAuthenticationMsApplicationTests;
import cl.tenpo.customerauthentication.api.dto.AbortChallengeRequest;
import cl.tenpo.customerauthentication.api.dto.AbortChallengeResponse;
import cl.tenpo.customerauthentication.dto.CustomerTransactionContextDTO;
import cl.tenpo.customerauthentication.exception.TenpoException;
import cl.tenpo.customerauthentication.externalservice.kafka.EventProducerService;
import cl.tenpo.customerauthentication.externalservice.kafka.dto.LockUnlockUserDto;
import cl.tenpo.customerauthentication.externalservice.user.UserRestClient;
import cl.tenpo.customerauthentication.externalservice.user.dto.UserResponse;
import cl.tenpo.customerauthentication.externalservice.user.dto.UserStateType;
import cl.tenpo.customerauthentication.model.ChallengeResult;
import cl.tenpo.customerauthentication.model.CustomerTransactionStatus;
import cl.tenpo.customerauthentication.properties.CustomerTransactionContextProperties;
import cl.tenpo.customerauthentication.service.Customer2faService;
import cl.tenpo.customerauthentication.service.CustomerChallengeService;
import com.fasterxml.jackson.core.JsonProcessingException;
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
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@SpringBootTest
public class Customer2faServiceAbortChallengeTests extends CustomerAuthenticationMsApplicationTests {

    @Autowired
    private Customer2faService customer2faService;

    @MockBean
    private CustomerChallengeService customerChallengeService;

    @MockBean
    private UserRestClient userRestClient;

    @MockBean
    private EventProducerService eventProducerService;

    @Autowired
    private CustomerTransactionContextProperties transactionContextProperties;

    @Test(expected = TenpoException.class)
    public void abortChallengeExtenalNotFound() throws JsonProcessingException {
        when(customerChallengeService.findByExternalId(anyString()))
                .thenReturn(Optional.empty());
        try{
            AbortChallengeRequest abortChallengeRequest = new AbortChallengeRequest();
            customer2faService.abortChallenge(UUID.randomUUID(),abortChallengeRequest);
            Assert.fail("Can't be here");
        }catch (TenpoException e){
            Assert.assertEquals("HttpStatus debe ser 404", HttpStatus.NOT_FOUND, e.getCode());
            Assert.assertEquals("Codigo debe ser 1100", EXTERNAL_ID_NOT_FOUND, e.getErrorCode());
            throw e;
        }
    }

    @Test(expected = TenpoException.class)
    public void abortChallengeTransactionAuthorized() throws JsonProcessingException {

        UserResponse userResponse = new UserResponse();
        userResponse.setId(UUID.randomUUID());
        userResponse.setState(UserStateType.ACTIVE);
        when(userRestClient.getUser(Mockito.any(UUID.class)))
                .thenReturn(Optional.of(userResponse));

        CustomerTransactionContextDTO customerTransactionContextDTO = createTransaction();
        customerTransactionContextDTO.setStatus(CustomerTransactionStatus.AUTHORIZED);
        when(customerChallengeService.findByExternalId(anyString()))
                .thenReturn(Optional.of(customerTransactionContextDTO));

        try {
            AbortChallengeRequest abortChallengeRequest = new AbortChallengeRequest();
            abortChallengeRequest.setExternalId(UUID.randomUUID().toString());
            customer2faService.abortChallenge(UUID.randomUUID(),abortChallengeRequest);
            Assert.fail("Can't be here");
        } catch (TenpoException e) {
            Assert.assertEquals("HttpStatus debe ser igual", HttpStatus.UNPROCESSABLE_ENTITY, e.getCode());
            Assert.assertEquals("Codigo debe ser 1202", TRANSACTION_CONTEXT_CLOSED, e.getErrorCode());
            throw e;
        }
    }

    @Test(expected = TenpoException.class)
    public void abortChallengeTransactionExpiredByStatus() throws JsonProcessingException {

        UserResponse userResponse = new UserResponse();
        userResponse.setId(UUID.randomUUID());
        userResponse.setState(UserStateType.ACTIVE);
        when(userRestClient.getUser(Mockito.any(UUID.class)))
                .thenReturn(Optional.of(userResponse));

        CustomerTransactionContextDTO customerTransactionContextDTO = createTransaction();
        customerTransactionContextDTO.setStatus(CustomerTransactionStatus.EXPIRED);
        when(customerChallengeService.findByExternalId(anyString()))
                .thenReturn(Optional.of(customerTransactionContextDTO));

        try {
            AbortChallengeRequest abortChallengeRequest = new AbortChallengeRequest();
            abortChallengeRequest.setExternalId(UUID.randomUUID().toString());
            customer2faService.abortChallenge(UUID.randomUUID(),abortChallengeRequest);
            Assert.fail("Can't be here");
        } catch (TenpoException e) {
            Assert.assertEquals("HttpStatus debe ser igual", HttpStatus.UNPROCESSABLE_ENTITY, e.getCode());
            Assert.assertEquals("Codigo debe ser 1200", TRANSACTION_CONTEXT_EXPIRED, e.getErrorCode());
            throw e;
        }
    }

    @Test(expected = TenpoException.class)
    public void abortChallengeTransactionExpiredByTime() throws JsonProcessingException {

        UserResponse userResponse = new UserResponse();
        userResponse.setId(UUID.randomUUID());
        userResponse.setState(UserStateType.ACTIVE);
        when(userRestClient.getUser(Mockito.any(UUID.class)))
                .thenReturn(Optional.of(userResponse));

        CustomerTransactionContextDTO customerTransactionContextDTO = createTransaction();
        customerTransactionContextDTO.setCreated(LocalDateTime.now(ZoneId.of("UTC")).minusMinutes(transactionContextProperties.getExpirationTimeInMinutes() + 1));
        when(customerChallengeService.findByExternalId(anyString()))
                .thenReturn(Optional.of(customerTransactionContextDTO));

        try {
            AbortChallengeRequest abortChallengeRequest = new AbortChallengeRequest();
            abortChallengeRequest.setExternalId(UUID.randomUUID().toString());
            customer2faService.abortChallenge(UUID.randomUUID(),abortChallengeRequest);
            Assert.fail("Can't be here");
        } catch (TenpoException e) {
            Assert.assertEquals("HttpStatus debe ser igual", HttpStatus.UNPROCESSABLE_ENTITY, e.getCode());
            Assert.assertEquals("Codigo debe ser 1200", TRANSACTION_CONTEXT_EXPIRED, e.getErrorCode());
            throw e;
        }
    }

    @Test(expected = TenpoException.class)
    public void abortChallengeTransactionRejectedByStatus() throws JsonProcessingException {

        UserResponse userResponse = new UserResponse();
        userResponse.setId(UUID.randomUUID());
        userResponse.setState(UserStateType.ACTIVE);
        when(userRestClient.getUser(Mockito.any(UUID.class)))
                .thenReturn(Optional.of(userResponse));

        CustomerTransactionContextDTO customerTransactionContextDTO = createTransaction();
        customerTransactionContextDTO.setStatus(CustomerTransactionStatus.REJECTED);
        when(customerChallengeService.findByExternalId(anyString()))
                .thenReturn(Optional.of(customerTransactionContextDTO));

        try {
            AbortChallengeRequest abortChallengeRequest = new AbortChallengeRequest();
            abortChallengeRequest.setExternalId(UUID.randomUUID().toString());
            customer2faService.abortChallenge(UUID.randomUUID(),abortChallengeRequest);
            Assert.fail("Can't be here");
        } catch (TenpoException e) {
            Assert.assertEquals("HttpStatus debe ser igual", HttpStatus.UNPROCESSABLE_ENTITY, e.getCode());
            Assert.assertEquals("Codigo debe ser 1151", BLOCKED_PASSWORD, e.getErrorCode());
            throw e;
        }
    }

    @Test(expected = TenpoException.class)
    public void abortChallengeTransactionRejectedByAttemps() throws JsonProcessingException {

        UserResponse userResponse = new UserResponse();
        userResponse.setId(UUID.randomUUID());
        userResponse.setState(UserStateType.ACTIVE);
        when(userRestClient.getUser(Mockito.any(UUID.class)))
                .thenReturn(Optional.of(userResponse));
        doNothing()
                .when(eventProducerService).sendLockEvent(Mockito.any(LockUnlockUserDto.class));
        CustomerTransactionContextDTO customerTransactionContextDTO = createTransaction();
        customerTransactionContextDTO.setStatus(CustomerTransactionStatus.PENDING);
        customerTransactionContextDTO.setAttempts(transactionContextProperties.getPasswordAttempts() + 1);
        when(customerChallengeService.findByExternalId(anyString()))
                .thenReturn(Optional.of(customerTransactionContextDTO));

        try {
            AbortChallengeRequest abortChallengeRequest = new AbortChallengeRequest();
            abortChallengeRequest.setExternalId(UUID.randomUUID().toString());
            customer2faService.abortChallenge(UUID.randomUUID(),abortChallengeRequest);
            Assert.fail("Can't be here");
        } catch (TenpoException e) {
            Assert.assertEquals("HttpStatus debe ser igual", HttpStatus.UNPROCESSABLE_ENTITY, e.getCode());
            Assert.assertEquals("Codigo debe ser 1151", BLOCKED_PASSWORD, e.getErrorCode());
            throw e;
        }
    }

    @Test(expected = TenpoException.class)
    public void abortChallengeUserRestClientThrowsException() throws JsonProcessingException {
        CustomerTransactionContextDTO customerTransactionContextDTO = createTransaction();
        when(customerChallengeService.findByExternalId(anyString()))
                .thenReturn(Optional.of(customerTransactionContextDTO));

        when(userRestClient.getUser(any(UUID.class)))
                .thenThrow(new NullPointerException());

        try {
            AbortChallengeRequest abortChallengeRequest = new AbortChallengeRequest();
            abortChallengeRequest.setExternalId(UUID.randomUUID().toString());
            customer2faService.abortChallenge(UUID.randomUUID(),abortChallengeRequest);
            Assert.fail("Can't be here");
        } catch (TenpoException e) {
            Assert.assertEquals("HttpStatus debe ser igual", HttpStatus.UNAUTHORIZED, e.getCode());
            Assert.assertEquals("Codigo debe ser igual", INVALID_TOKEN, e.getErrorCode());
            throw e;
        }
    }

    @Test(expected = TenpoException.class)
    public void abortChallengeUserNotFound() throws JsonProcessingException {
        CustomerTransactionContextDTO customerTransactionContextDTO = createTransaction();
        when(customerChallengeService.findByExternalId(anyString()))
                .thenReturn(Optional.of(customerTransactionContextDTO));

        when(userRestClient.getUser(any(UUID.class)))
                .thenReturn(Optional.empty());

        try {
            AbortChallengeRequest abortChallengeRequest = new AbortChallengeRequest();
            abortChallengeRequest.setExternalId(UUID.randomUUID().toString());
            customer2faService.abortChallenge(UUID.randomUUID(),abortChallengeRequest);
            Assert.fail("Can't be here");
        } catch (TenpoException e) {
            Assert.assertEquals("HttpStatus debe ser igual", HttpStatus.UNAUTHORIZED, e.getCode());
            Assert.assertEquals("Codigo debe ser igual", INVALID_TOKEN, e.getErrorCode());
            throw e;
        }
    }

    @Test(expected = TenpoException.class)
    public void abortChallengeUserNoActive() throws JsonProcessingException {
        CustomerTransactionContextDTO customerTransactionContextDTO = createTransaction();
        when(customerChallengeService.findByExternalId(anyString()))
                .thenReturn(Optional.of(customerTransactionContextDTO));

        UserResponse userResponse = new UserResponse();
        userResponse.setId(UUID.randomUUID());
        userResponse.setState(UserStateType.BLOCKED);

        when(userRestClient.getUser(any(UUID.class)))
                .thenReturn(Optional.of(userResponse));

        try {
            AbortChallengeRequest abortChallengeRequest = new AbortChallengeRequest();
            abortChallengeRequest.setExternalId(UUID.randomUUID().toString());
            customer2faService.abortChallenge(UUID.randomUUID(),abortChallengeRequest);
            Assert.fail("Can't be here");
        } catch (TenpoException e) {
            Assert.assertEquals("HttpStatus debe ser igual", HttpStatus.UNAUTHORIZED, e.getCode());
            Assert.assertEquals("Codigo debe ser igual", INVALID_TOKEN, e.getErrorCode());
            throw e;
        }
    }

    @Test
    public void abortChallengeOK() throws JsonProcessingException {
        CustomerTransactionContextDTO customerTransactionContextDTO = createTransaction();
        when(customerChallengeService.findByExternalId(anyString()))
                .thenReturn(Optional.of(customerTransactionContextDTO));

        UserResponse userResponse = new UserResponse();
        userResponse.setId(UUID.randomUUID());
        userResponse.setState(UserStateType.ACTIVE);

        when(userRestClient.getUser(any(UUID.class)))
                .thenReturn(Optional.of(userResponse));

        when(customerChallengeService.updateTransactionContextStatus(any(), any()))
                .thenReturn(Optional.of(customerTransactionContextDTO));

        try {
            AbortChallengeRequest abortChallengeRequest = new AbortChallengeRequest();
            abortChallengeRequest.setExternalId(UUID.randomUUID().toString());

            AbortChallengeResponse response = customer2faService.abortChallenge(UUID.randomUUID(),abortChallengeRequest);
            Assert.assertEquals("Debe tener el mismo external id", customerTransactionContextDTO.getExternalId(), response.getExternalId());
            Assert.assertEquals("Debe estar cancelado", ChallengeResult.CANCELADO, response.getResult());

        } catch (TenpoException e) {
            Assert.fail("Can't be here");
            throw e;
        }
    }

    @Test
    public void abortCancelledChallengeOK() throws JsonProcessingException {
        CustomerTransactionContextDTO customerTransactionContextDTO = createTransaction();
        customerTransactionContextDTO.setStatus(CustomerTransactionStatus.CANCEL); // Challenge ya cancelado
        when(customerChallengeService.findByExternalId(anyString()))
                .thenReturn(Optional.of(customerTransactionContextDTO));

        UserResponse userResponse = new UserResponse();
        userResponse.setId(UUID.randomUUID());
        userResponse.setState(UserStateType.ACTIVE);

        when(userRestClient.getUser(any(UUID.class)))
                .thenReturn(Optional.of(userResponse));

        when(customerChallengeService.updateTransactionContextStatus(any(), any()))
                .thenReturn(Optional.of(customerTransactionContextDTO));

        try {
            AbortChallengeRequest abortChallengeRequest = new AbortChallengeRequest();
            abortChallengeRequest.setExternalId(UUID.randomUUID().toString());

            AbortChallengeResponse response = customer2faService.abortChallenge(UUID.randomUUID(),abortChallengeRequest);
            Assert.assertEquals("Debe tener el mismo external id", customerTransactionContextDTO.getExternalId(), response.getExternalId());
            Assert.assertEquals("Debe estar cancelado", ChallengeResult.CANCELADO, response.getResult());

        } catch (TenpoException e) {
            Assert.fail("Can't be here");
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
