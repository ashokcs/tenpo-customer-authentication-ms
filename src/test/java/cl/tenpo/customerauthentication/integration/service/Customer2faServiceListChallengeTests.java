package cl.tenpo.customerauthentication.integration.service;

import cl.tenpo.customerauthentication.CustomerAuthenticationMsApplicationTests;
import cl.tenpo.customerauthentication.constants.ErrorCode;
import cl.tenpo.customerauthentication.exception.TenpoException;
import cl.tenpo.customerauthentication.externalservice.user.UserRestClientImpl;
import cl.tenpo.customerauthentication.externalservice.user.dto.UserResponse;
import cl.tenpo.customerauthentication.externalservice.user.dto.UserStateType;
import cl.tenpo.customerauthentication.service.Customer2faService;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@SpringBootTest
@Slf4j
public class Customer2faServiceListChallengeTests  extends CustomerAuthenticationMsApplicationTests {

    @Autowired
    private Customer2faService customer2faService;

    @MockBean
    private UserRestClientImpl userRestClient;

    @Test
    public void listChallengeOk(){

        final UUID userId = UUID.randomUUID();

        when(userRestClient.getUser(eq(userId)))
                .thenReturn(getUserResponse(userId, UserStateType.ACTIVE));

        List<String> challengeList = customer2faService.listChallenge(userId);
        Assert.assertNotNull("La lista no debe ser null", challengeList);
        Assert.assertEquals("Debe contener 4 elementos", 4, challengeList.size());
    }

    @Test(expected = TenpoException.class)
    public void listChallengeUserNotFound() {

        when(userRestClient.getUser(any(UUID.class)))
                .thenReturn(Optional.empty());

       try {
          customer2faService.listChallenge(UUID.randomUUID());
          Assert.fail("Can't be here");
       }catch (TenpoException e){
           Assert.assertEquals("El codigo debe ser 401", HttpStatus.UNAUTHORIZED, e.getCode());
           Assert.assertEquals("El codigo debe ser 1150", ErrorCode.INVALID_TOKEN, e.getErrorCode());
           throw  e;
       }
    }


    @Test(expected = TenpoException.class)
    public void listChallengeUserLocked() {

        final UUID userId = UUID.randomUUID();

        when(userRestClient.getUser(eq(userId)))
                .thenReturn(getUserResponse(userId, UserStateType.BLOCKED));

        try {
            customer2faService.listChallenge(userId);
            Assert.fail("Can't be here");
        }catch (TenpoException e){
            Assert.assertEquals("El codigo debe ser 401", HttpStatus.UNAUTHORIZED, e.getCode());
            Assert.assertEquals("El codigo debe ser 1150", ErrorCode.INVALID_TOKEN, e.getErrorCode());
            throw  e;
        }
    }

    private Optional<UserResponse> getUserResponse(UUID userId, UserStateType status) {
        UserResponse userResponse = new UserResponse();
        userResponse.setId(userId);
        userResponse.setTributaryIdentifier("1-9");
        userResponse.setState(status);

        return Optional.of(userResponse);
    }


}
