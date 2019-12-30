package cl.tenpo.customerauthentication.unit.service;

import cl.tenpo.customerauthentication.CustomerAuthenticationMsApplicationTests;
import cl.tenpo.customerauthentication.api.dto.ValidateChallengeRequest;
import cl.tenpo.customerauthentication.api.dto.ValidateChallengeResponse;
import cl.tenpo.customerauthentication.dto.CustomerTransactionContextDTO;
import cl.tenpo.customerauthentication.exception.TenpoException;
import cl.tenpo.customerauthentication.externalservice.user.UserRestClient;
import cl.tenpo.customerauthentication.externalservice.user.dto.UserResponse;
import cl.tenpo.customerauthentication.externalservice.user.dto.UserStateType;
import cl.tenpo.customerauthentication.externalservice.verifier.VerifierRestClient;
import cl.tenpo.customerauthentication.model.ChallengeResult;
import cl.tenpo.customerauthentication.model.CustomerTransactionStatus;
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

import java.util.Optional;
import java.util.UUID;

import static cl.tenpo.customerauthentication.constants.ErrorCode.*;
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

    @Test(expected = TenpoException.class)
    public void validateChallengeExtenalNotFound(){
        when(customerChallengeService.findByExternalId(Mockito.any(UUID.class)))
                .thenReturn(Optional.empty());
        try{
            ValidateChallengeRequest validateChallengeRequest = new ValidateChallengeRequest();
            customer2faService.validateChallenge(UUID.randomUUID(), validateChallengeRequest);
            Assert.fail("Can't be here");
        }catch (TenpoException e){
            Assert.assertEquals("HttpStatus debe ser igual", HttpStatus.UNPROCESSABLE_ENTITY, e.getCode());
            Assert.assertEquals("Codigo debe ser igual", EXTERNAL_ID_NOT_FOUND, e.getErrorCode());
            Assert.assertEquals("Msj debe ser igual","No se encontró el external_id. Llame primero a POST",e.getMessage());
            throw e;
        }
    }

    @Test(expected = TenpoException.class)
    public void validateChallengeTrxContextRejected() {

        CustomerTransactionContextDTO customerTransactionContextDTO = new CustomerTransactionContextDTO();
        customerTransactionContextDTO.setStatus(CustomerTransactionStatus.REJECTED);
        customerTransactionContextDTO.setAttempts(0);

        when(customerChallengeService.findByExternalId(Mockito.any(UUID.class)))
                .thenReturn(Optional.of(customerTransactionContextDTO));

        UserResponse userResponse = new UserResponse();
        userResponse.setId(UUID.randomUUID());
        userResponse.setState(UserStateType.BLOCKED);

        when(userRestClient.getUser(Mockito.any(UUID.class)))
                .thenReturn(Optional.of(userResponse));

        try{
            ValidateChallengeRequest validateChallengeRequest = new ValidateChallengeRequest();
            validateChallengeRequest.setExternalId(UUID.randomUUID());
            customer2faService.validateChallenge(UUID.randomUUID(), validateChallengeRequest);
            Assert.fail("Can't be here");
        }catch (TenpoException e){
            Assert.assertEquals("HttpStatus debe ser igual", HttpStatus.UNPROCESSABLE_ENTITY, e.getCode());
            Assert.assertEquals("Codigo debe ser igual", TRANSACTION_CONTEXT_LOCKED, e.getErrorCode());
            Assert.assertEquals("Msj debe ser igual","Transaccion bloqueada por intentos",e.getMessage());
            throw e;
        }
    }

    @Test(expected = TenpoException.class)
    public void validateChallengeUserNotFound() {

        CustomerTransactionContextDTO customerTransactionContextDTO = new CustomerTransactionContextDTO();
        customerTransactionContextDTO.setStatus(CustomerTransactionStatus.PENDING);
        customerTransactionContextDTO.setAttempts(0);

        when(customerChallengeService.findByExternalId(Mockito.any(UUID.class)))
                .thenReturn(Optional.of(customerTransactionContextDTO));

        when(userRestClient.getUser(Mockito.any(UUID.class)))
                .thenReturn(Optional.empty());

        try{
            ValidateChallengeRequest validateChallengeRequest = new ValidateChallengeRequest();
            validateChallengeRequest.setExternalId(UUID.randomUUID());
            customer2faService.validateChallenge(UUID.randomUUID(), validateChallengeRequest);
            Assert.fail("Can't be here");
        }catch (TenpoException e){
            Assert.assertEquals("HttpStatus debe ser igual", HttpStatus.NOT_FOUND, e.getCode());
            Assert.assertEquals("Codigo debe ser igual", USER_NOT_FOUND_OR_LOCKED, e.getErrorCode());
            Assert.assertEquals("Msj debe ser igual","El cliente no existe o está bloqueado",e.getMessage());
            throw e;
        }
    }

    @Test(expected = TenpoException.class)
    public void validateChallengeUserNoActive() {

        CustomerTransactionContextDTO customerTransactionContextDTO = new CustomerTransactionContextDTO();
        customerTransactionContextDTO.setStatus(CustomerTransactionStatus.PENDING);
        customerTransactionContextDTO.setAttempts(0);

        when(customerChallengeService.findByExternalId(Mockito.any(UUID.class)))
                .thenReturn(Optional.of(customerTransactionContextDTO));

        UserResponse userResponse = new UserResponse();
        userResponse.setId(UUID.randomUUID());
        userResponse.setState(UserStateType.BLOCKED);

        when(userRestClient.getUser(Mockito.any(UUID.class)))
                .thenReturn(Optional.of(userResponse));

        try{
            ValidateChallengeRequest validateChallengeRequest = new ValidateChallengeRequest();
            validateChallengeRequest.setExternalId(UUID.randomUUID());
            customer2faService.validateChallenge(UUID.randomUUID(), validateChallengeRequest);
            Assert.fail("Can't be here");
        }catch (TenpoException e){
            Assert.assertEquals("HttpStatus debe ser igual", HttpStatus.NOT_FOUND, e.getCode());
            Assert.assertEquals("Codigo debe ser igual", USER_NOT_FOUND_OR_LOCKED, e.getErrorCode());
            Assert.assertEquals("Msj debe ser igual","El cliente no existe o está bloqueado",e.getMessage());
            throw e;
        }
    }

    @Test
    public void validateChallengeOK() {

        CustomerTransactionContextDTO customerTransactionContextDTO = new CustomerTransactionContextDTO();
        customerTransactionContextDTO.setStatus(CustomerTransactionStatus.PENDING);
        customerTransactionContextDTO.setAttempts(0);
        customerTransactionContextDTO.setExternalId(UUID.randomUUID());

        when(customerChallengeService.findByExternalId(Mockito.any(UUID.class)))
                .thenReturn(Optional.of(customerTransactionContextDTO));

        UserResponse userResponse = new UserResponse();
        userResponse.setId(UUID.randomUUID());
        userResponse.setState(UserStateType.ACTIVE);

        when(userRestClient.getUser(Mockito.any(UUID.class)))
                .thenReturn(Optional.of(userResponse));

        when(verifierRestClient.validateTwoFactorCode(Mockito.any(UUID.class),Mockito.anyString()))
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

        CustomerTransactionContextDTO customerTransactionContextDTO = new CustomerTransactionContextDTO();
        customerTransactionContextDTO.setStatus(CustomerTransactionStatus.PENDING);
        customerTransactionContextDTO.setAttempts(0);
        customerTransactionContextDTO.setExternalId(UUID.randomUUID());

        when(customerChallengeService.findByExternalId(Mockito.any(UUID.class)))
                .thenReturn(Optional.of(customerTransactionContextDTO));

        UserResponse userResponse = new UserResponse();
        userResponse.setId(UUID.randomUUID());
        userResponse.setState(UserStateType.ACTIVE);

        when(userRestClient.getUser(Mockito.any(UUID.class)))
                .thenReturn(Optional.of(userResponse));

        when(verifierRestClient.validateTwoFactorCode(Mockito.any(UUID.class),Mockito.anyString()))
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
}
