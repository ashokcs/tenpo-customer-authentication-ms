package cl.tenpo.customerauthentication.unit.service;


import cl.tenpo.customerauthentication.CustomerAuthenticationMsApplicationTests;
import cl.tenpo.customerauthentication.api.dto.AbortChallengeRequest;
import cl.tenpo.customerauthentication.dto.CustomerTransactionContextDTO;
import cl.tenpo.customerauthentication.exception.TenpoException;
import cl.tenpo.customerauthentication.externalservice.user.UserRestClient;
import cl.tenpo.customerauthentication.externalservice.user.dto.UserResponse;
import cl.tenpo.customerauthentication.externalservice.user.dto.UserStateType;
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

import static cl.tenpo.customerauthentication.constants.ErrorCode.EXTERNAL_ID_NOT_FOUND;
import static cl.tenpo.customerauthentication.constants.ErrorCode.USER_NOT_FOUND_OR_LOCKED;
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

    @Test(expected = TenpoException.class)
    public void abortChallengeExtenalNotFound(){
        when(customerChallengeService.findByExternalId(Mockito.any(UUID.class)))
                .thenReturn(Optional.empty());
        try{
            AbortChallengeRequest abortChallengeRequest = new AbortChallengeRequest();
            customer2faService.abortResponse(UUID.randomUUID(),abortChallengeRequest);
            Assert.fail("Can't be here");
        }catch (TenpoException e){
            Assert.assertEquals("HttpStatus debe ser igual", HttpStatus.UNPROCESSABLE_ENTITY, e.getCode());
            Assert.assertEquals("Codigo debe ser igual", EXTERNAL_ID_NOT_FOUND, e.getErrorCode());
            Assert.assertEquals("Msj debe ser igual","No se encontró el external_id. Llame primero a POST",e.getMessage());
            throw e;
        }
    }

    @Test(expected = TenpoException.class)
    public void abortChallengeUserNotFound() {
        when(customerChallengeService.findByExternalId(Mockito.any(UUID.class)))
                .thenReturn(Optional.of(new CustomerTransactionContextDTO()));

        when(userRestClient.getUser(Mockito.any(UUID.class)))
                .thenReturn(Optional.empty());

        try{
            AbortChallengeRequest abortChallengeRequest = new AbortChallengeRequest();
            abortChallengeRequest.setExternalId(UUID.randomUUID());
            customer2faService.abortResponse(UUID.randomUUID(),abortChallengeRequest);
            Assert.fail("Can't be here");
        }catch (TenpoException e){
            Assert.assertEquals("HttpStatus debe ser igual", HttpStatus.NOT_FOUND, e.getCode());
            Assert.assertEquals("Codigo debe ser igual", USER_NOT_FOUND_OR_LOCKED, e.getErrorCode());
            Assert.assertEquals("Msj debe ser igual","El cliente no existe o está bloqueado",e.getMessage());
            throw e;
        }
    }

    @Test(expected = TenpoException.class)
    public void abortChallengeUserNoActive() {

        when(customerChallengeService.findByExternalId(Mockito.any(UUID.class)))
                .thenReturn(Optional.of(new CustomerTransactionContextDTO()));

        UserResponse userResponse = new UserResponse();
        userResponse.setId(UUID.randomUUID());
        userResponse.setState(UserStateType.BLOCKED);

        when(userRestClient.getUser(Mockito.any(UUID.class)))
                .thenReturn(Optional.of(userResponse));

        try{
            AbortChallengeRequest abortChallengeRequest = new AbortChallengeRequest();
            abortChallengeRequest.setExternalId(UUID.randomUUID());
            customer2faService.abortResponse(UUID.randomUUID(),abortChallengeRequest);
            Assert.fail("Can't be here");
        }catch (TenpoException e){
            Assert.assertEquals("HttpStatus debe ser igual", HttpStatus.NOT_FOUND, e.getCode());
            Assert.assertEquals("Codigo debe ser igual", USER_NOT_FOUND_OR_LOCKED, e.getErrorCode());
            Assert.assertEquals("Msj debe ser igual","El cliente no existe o está bloqueado",e.getMessage());
            throw e;
        }
    }

    @Test
    public void abortChallengeOK() {

        when(customerChallengeService.findByExternalId(Mockito.any(UUID.class)))
                .thenReturn(Optional.of(new CustomerTransactionContextDTO()));

        UserResponse userResponse = new UserResponse();
        userResponse.setId(UUID.randomUUID());
        userResponse.setState(UserStateType.ACTIVE);

        when(userRestClient.getUser(Mockito.any(UUID.class)))
                .thenReturn(Optional.of(userResponse));

        CustomerTransactionContextDTO customerTransactionContextDTO = new CustomerTransactionContextDTO();
        customerTransactionContextDTO.setId(UUID.randomUUID());

        when(customerChallengeService.updateTransactionContextStatus(Mockito.any(UUID.class),Mockito.any(CustomerTransactionStatus.class)))
                .thenReturn(Optional.of(customerTransactionContextDTO));

        try{
            AbortChallengeRequest abortChallengeRequest = new AbortChallengeRequest();
            abortChallengeRequest.setExternalId(UUID.randomUUID());

            customer2faService.abortResponse(UUID.randomUUID(),abortChallengeRequest);

        }catch (TenpoException e){
            Assert.fail("Can't be here");
            throw e;
        }
    }

}
