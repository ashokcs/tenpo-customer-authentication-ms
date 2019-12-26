package cl.tenpo.customerauthentication.integration.service;

import cl.tenpo.customerauthentication.CustomerAuthenticationMsApplicationTests;
import cl.tenpo.customerauthentication.exception.TenpoException;
import cl.tenpo.customerauthentication.externalservice.user.UserRestClientMock;
import cl.tenpo.customerauthentication.service.Customer2faService;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;
import java.util.UUID;

@RunWith(SpringRunner.class)
@SpringBootTest
@Slf4j
public class Customer2faServiceListChallengeTests  extends CustomerAuthenticationMsApplicationTests {

    @Autowired
    private Customer2faService customer2faService;

    @Test
    public void listChallengeOk(){
        List<String> challengeList = customer2faService.listChallenge(UUID.randomUUID());
        Assert.assertNotNull("La lista no debe ser null", challengeList);
        Assert.assertEquals("Debe contener 4 elementos", 4,challengeList.size());
    }

    @Test(expected = TenpoException.class)
    public void listChallengeUserNotFound() {
       try {
          customer2faService.listChallenge(UserRestClientMock.USER_NOT_FOUND);
          Assert.fail("Can't be here");
       }catch (TenpoException e){
           Assert.assertEquals("El codigo debe ser 1150","1150",e.getErrorCode());
           Assert.assertEquals("El msj debe ser -El cliente no existe o está bloqueado-","El cliente no existe o está bloqueado",e.getMessage());
           throw  e;
       }
    }


    @Test(expected = TenpoException.class)
    public void listChallengeUseBloqued() {
        try {
            customer2faService.listChallenge(UserRestClientMock.USER_BLOQUED);
            Assert.fail("Can't be here");
        }catch (TenpoException e){
            Assert.assertEquals("El codigo debe ser 1150","1150",e.getErrorCode());
            Assert.assertEquals("El msj debe ser -El cliente no existe o está bloqueado-","El cliente no existe o está bloqueado",e.getMessage());
            throw  e;
        }
    }



}
