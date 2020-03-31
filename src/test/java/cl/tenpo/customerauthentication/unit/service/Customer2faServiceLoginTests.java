package cl.tenpo.customerauthentication.unit.service;


import cl.tenpo.customerauthentication.CustomerAuthenticationMsApplicationTests;
import cl.tenpo.customerauthentication.api.dto.CustomerLoginRequest;
import cl.tenpo.customerauthentication.constants.ErrorCode;
import cl.tenpo.customerauthentication.exception.TenpoException;
import cl.tenpo.customerauthentication.externalservice.cards.CardRestClient;
import cl.tenpo.customerauthentication.externalservice.login.LoginRestClient;
import cl.tenpo.customerauthentication.externalservice.login.dto.LoginRequestDTO;
import cl.tenpo.customerauthentication.externalservice.login.dto.LoginResponseDTO;
import cl.tenpo.customerauthentication.externalservice.user.UserRestClient;
import cl.tenpo.customerauthentication.externalservice.user.dto.UserResponse;
import cl.tenpo.customerauthentication.externalservice.user.dto.UserStateType;
import cl.tenpo.customerauthentication.service.Customer2faService;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.HttpClientErrorException;

import java.io.IOException;
import java.text.ParseException;
import java.util.Optional;
import java.util.UUID;

import static cl.tenpo.customerauthentication.constants.ErrorCode.INVALID_CREDENTIALS;
import static cl.tenpo.customerauthentication.constants.ErrorCode.INVALID_PAN;
import static org.mockito.Mockito.*;


@RunWith(SpringRunner.class)
@SpringBootTest
public class Customer2faServiceLoginTests extends CustomerAuthenticationMsApplicationTests {

    @Autowired
    private Customer2faService customer2faService;

    @MockBean
    private LoginRestClient loginRestClient;

    @MockBean
    private UserRestClient userRestClient;

    @MockBean
    private CardRestClient cardRestClient;

    @Test(expected = TenpoException.class)
    public void login_WhenEmailNull_ThenThrowsException() throws IOException, ParseException {
        CustomerLoginRequest customerLoginRequest = CustomerLoginRequest.builder()
                .email(null)
                .pan("123456XXXXXX1234")
                .clave("1234")
                .build();

        try {
            customer2faService.login(customerLoginRequest);
        } catch (TenpoException te) {
            Assert.assertEquals("Debe tirar status 400", HttpStatus.BAD_REQUEST, te.getCode());
            Assert.assertEquals("Debe tirar error 1400", ErrorCode.MISSING_PARAMETERS, te.getErrorCode());
            throw te;
        }
    }

    @Test(expected = TenpoException.class)
    public void login_WhenEmailEmpty_ThenThrowsException() throws IOException, ParseException {
        CustomerLoginRequest customerLoginRequest = CustomerLoginRequest.builder()
                .email("")
                .pan("123456XXXXXX1234")
                .clave("1234")
                .build();

        try {
            customer2faService.login(customerLoginRequest);
        } catch (TenpoException te) {
            Assert.assertEquals("Debe tirar status 400", HttpStatus.BAD_REQUEST, te.getCode());
            Assert.assertEquals("Debe tirar error 1400", ErrorCode.MISSING_PARAMETERS, te.getErrorCode());
            throw te;
        }
    }

    @Test(expected = TenpoException.class)
    public void login_WhenPanNull_ThenThrowsException() throws IOException, ParseException {
        CustomerLoginRequest customerLoginRequest = CustomerLoginRequest.builder()
                .email("hola@mail.com")
                .pan(null)
                .clave("1234")
                .build();

        try {
            customer2faService.login(customerLoginRequest);
        } catch (TenpoException te) {
            Assert.assertEquals("Debe tirar status 400", HttpStatus.BAD_REQUEST, te.getCode());
            Assert.assertEquals("Debe tirar error 1400", ErrorCode.MISSING_PARAMETERS, te.getErrorCode());
            throw te;
        }
    }

    @Test(expected = TenpoException.class)
    public void login_WhenPanEmpty_ThenThrowsException() throws IOException, ParseException {
        CustomerLoginRequest customerLoginRequest = CustomerLoginRequest.builder()
                .email("hola@mail.com")
                .pan("")
                .clave("1234")
                .build();

        try {
            customer2faService.login(customerLoginRequest);
        } catch (TenpoException te) {
            Assert.assertEquals("Debe tirar status 400", HttpStatus.BAD_REQUEST, te.getCode());
            Assert.assertEquals("Debe tirar error 1400", ErrorCode.MISSING_PARAMETERS, te.getErrorCode());
            throw te;
        }
    }

    @Test(expected = TenpoException.class)
    public void login_WhenPasswordNull_ThenThrowsException() throws IOException, ParseException {
        CustomerLoginRequest customerLoginRequest = CustomerLoginRequest.builder()
                .email("hola@mail.com")
                .pan("123456XXXXXX1234")
                .clave(null)
                .build();

        try {
            customer2faService.login(customerLoginRequest);
        } catch (TenpoException te) {
            Assert.assertEquals("Debe tirar status 400", HttpStatus.BAD_REQUEST, te.getCode());
            Assert.assertEquals("Debe tirar error 1400", ErrorCode.MISSING_PARAMETERS, te.getErrorCode());
            throw te;
        }
    }

    @Test(expected = TenpoException.class)
    public void login_WhenPasswordEmpty_ThenThrowsException() throws IOException, ParseException {
        CustomerLoginRequest customerLoginRequest = CustomerLoginRequest.builder()
                .email("hola@mail.com")
                .pan("123456XXXXXX1234")
                .clave("")
                .build();

        try {
            customer2faService.login(customerLoginRequest);
        } catch (TenpoException te) {
            Assert.assertEquals("Debe tirar status 400", HttpStatus.BAD_REQUEST, te.getCode());
            Assert.assertEquals("Debe tirar error 1400", ErrorCode.MISSING_PARAMETERS, te.getErrorCode());
            throw te;
        }
    }

    @Test(expected = TenpoException.class)
    public void loginErrorAzureLogin() throws IOException, ParseException {

        when(loginRestClient.login(Mockito.any(LoginRequestDTO.class)))
                .thenThrow(new TenpoException(HttpStatus.NOT_FOUND,INVALID_CREDENTIALS));
        try {
            customer2faService.login(CustomerLoginRequest
                    .builder()
                    .email("test@email.com")
                    .clave("1234")
                    .pan("5555XXXXX55555")
                    .build());
            Assert.fail("Can't be here");

        }catch (TenpoException e){
            Assert.assertEquals("HttpStatus debe ser igual", HttpStatus.NOT_FOUND, e.getCode());
            Assert.assertEquals("Codigo debe ser igual", INVALID_CREDENTIALS, e.getErrorCode());
            throw e;
        }
    }

    @Test(expected = TenpoException.class)
    public void loginErrorUserLocked() throws IOException, ParseException {

        LoginResponseDTO tokenResponse = new LoginResponseDTO();
        tokenResponse.setAccessToken("eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiIsImtpZCI6Ilg1ZVhrNHh5b2pORnVtMWtsMll0djhkbE5QNC1jNTdkTzZRR1RWQndhTmsifQ.eyJpc3MiOiJodHRwczovL3RlbnBvZGV2Mi5iMmNsb2dpbi5jb20vY2FlMDlmZWUtNDczZS00OGUyLTk5ZGMtM2I3YmY0YTk3NDExL3YyLjAvIiwiZXhwIjoxNTc3NzEzMTI1LCJuYmYiOjE1Nzc3MTI4MjUsImF1ZCI6IjNiMWI3MTMzLTM4OWYtNGUxYi04MTE3LTI3YjdiYWY2ZGZkMiIsImlkcCI6IkxvY2FsQWNjb3VudCIsIm9pZCI6IjAzZjdlMGNkLWEzMDUtNGUyNC1hODA2LTE0MzhiZWI2ZmRhYiIsInN1YiI6IjAzZjdlMGNkLWEzMDUtNGUyNC1hODA2LTE0MzhiZWI2ZmRhYiIsIm5hbWUiOiJCQVNUSUFOIEhFUk5BTkRFWiBXSUxTT04iLCJlbWFpbHMiOlsiYmFzdGlhbi5oZXJuYW5kZXpAdGVucG8uY2wiXSwidGZwIjoiQjJDXzFfd2VicGF5IiwiYXpwIjoiM2IxYjcxMzMtMzg5Zi00ZTFiLTgxMTctMjdiN2JhZjZkZmQyIiwidmVyIjoiMS4wIiwiaWF0IjoxNTc3NzEyODI1fQ.sKtw2dfAFlHi1PW3jnKeKxbVQXNwqwJ_7ar_cDMs74ltOXHJ0jXytW3S-EObbyLYMgfIW1cnEw_xcyVq5dK1hXvr0nhLt2UEM4t7drrciPaOpMF5uNKNlW6u2zfb963RdPBcAAzRk3Zl11aE0PBrjEzG7RV1ZkX79QYp4H_-j0-kriQ42Pj05d8lK8-79aamSAgfiBGcjBUHPecYOwxvURCK76Zf6YvKRagIUylIQA7lJTP5VuVho6U4kJAOzjoEJqLLgvySiYuA5h6Jm3EDWRQfXUE_7vfkBuSmUwoYQZcEYkGJrcZOsZ8RhLrcIXsYZeudxeGhvFugF9zt3VYgpQ");
        when(loginRestClient.login(Mockito.any(LoginRequestDTO.class)))
                .thenReturn(tokenResponse);

        UserResponse userResponse = new UserResponse();
        userResponse.setState(UserStateType.BLOCKED);

        when(userRestClient.getUserByProvider(Mockito.any(UUID.class)))
                .thenReturn(Optional.of(userResponse));

        try {
            customer2faService.login(CustomerLoginRequest
                    .builder()
                    .email("test@email.com")
                    .clave("1234")
                    .pan("5555XXXXX55555")
                    .build());
            Assert.fail("Can't be here");

        }catch (TenpoException e){
            Assert.assertEquals("HttpStatus debe ser igual", HttpStatus.NOT_FOUND, e.getCode());
            Assert.assertEquals("Codigo debe ser igual", INVALID_CREDENTIALS, e.getErrorCode());
            throw e;
        }
    }

    @Test(expected = TenpoException.class)
    public void loginErrorUserLocked2() throws IOException, ParseException {

        LoginResponseDTO tokenResponse = new LoginResponseDTO();
        tokenResponse.setAccessToken("eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiIsImtpZCI6Ilg1ZVhrNHh5b2pORnVtMWtsMll0djhkbE5QNC1jNTdkTzZRR1RWQndhTmsifQ.eyJpc3MiOiJodHRwczovL3RlbnBvZGV2Mi5iMmNsb2dpbi5jb20vY2FlMDlmZWUtNDczZS00OGUyLTk5ZGMtM2I3YmY0YTk3NDExL3YyLjAvIiwiZXhwIjoxNTc3NzEzMTI1LCJuYmYiOjE1Nzc3MTI4MjUsImF1ZCI6IjNiMWI3MTMzLTM4OWYtNGUxYi04MTE3LTI3YjdiYWY2ZGZkMiIsImlkcCI6IkxvY2FsQWNjb3VudCIsIm9pZCI6IjAzZjdlMGNkLWEzMDUtNGUyNC1hODA2LTE0MzhiZWI2ZmRhYiIsInN1YiI6IjAzZjdlMGNkLWEzMDUtNGUyNC1hODA2LTE0MzhiZWI2ZmRhYiIsIm5hbWUiOiJCQVNUSUFOIEhFUk5BTkRFWiBXSUxTT04iLCJlbWFpbHMiOlsiYmFzdGlhbi5oZXJuYW5kZXpAdGVucG8uY2wiXSwidGZwIjoiQjJDXzFfd2VicGF5IiwiYXpwIjoiM2IxYjcxMzMtMzg5Zi00ZTFiLTgxMTctMjdiN2JhZjZkZmQyIiwidmVyIjoiMS4wIiwiaWF0IjoxNTc3NzEyODI1fQ.sKtw2dfAFlHi1PW3jnKeKxbVQXNwqwJ_7ar_cDMs74ltOXHJ0jXytW3S-EObbyLYMgfIW1cnEw_xcyVq5dK1hXvr0nhLt2UEM4t7drrciPaOpMF5uNKNlW6u2zfb963RdPBcAAzRk3Zl11aE0PBrjEzG7RV1ZkX79QYp4H_-j0-kriQ42Pj05d8lK8-79aamSAgfiBGcjBUHPecYOwxvURCK76Zf6YvKRagIUylIQA7lJTP5VuVho6U4kJAOzjoEJqLLgvySiYuA5h6Jm3EDWRQfXUE_7vfkBuSmUwoYQZcEYkGJrcZOsZ8RhLrcIXsYZeudxeGhvFugF9zt3VYgpQ");
        when(loginRestClient.login(Mockito.any(LoginRequestDTO.class)))
                .thenReturn(tokenResponse);

        UserResponse userResponse = new UserResponse();
        userResponse.setState(UserStateType.BLOCKED);

        when(userRestClient.getUserByProvider(Mockito.any(UUID.class)))
                .thenReturn(Optional.empty());

        try {
            customer2faService.login(CustomerLoginRequest
                    .builder()
                    .email("test@email.com")
                    .clave("1234")
                    .pan("5555XXXXX55555")
                    .build());
            Assert.fail("Can't be here");

        }catch (TenpoException e){
            Assert.assertEquals("HttpStatus debe ser igual", HttpStatus.NOT_FOUND, e.getCode());
            Assert.assertEquals("Codigo debe ser igual", INVALID_CREDENTIALS, e.getErrorCode());
            throw e;
        }
    }

    @Test(expected = TenpoException.class)
    public void loginErrorCardNotFound() throws IOException, ParseException {

        LoginResponseDTO tokenResponse = new LoginResponseDTO();
        tokenResponse.setAccessToken("eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiIsImtpZCI6Ilg1ZVhrNHh5b2pORnVtMWtsMll0djhkbE5QNC1jNTdkTzZRR1RWQndhTmsifQ.eyJpc3MiOiJodHRwczovL3RlbnBvZGV2Mi5iMmNsb2dpbi5jb20vY2FlMDlmZWUtNDczZS00OGUyLTk5ZGMtM2I3YmY0YTk3NDExL3YyLjAvIiwiZXhwIjoxNTc3NzEzMTI1LCJuYmYiOjE1Nzc3MTI4MjUsImF1ZCI6IjNiMWI3MTMzLTM4OWYtNGUxYi04MTE3LTI3YjdiYWY2ZGZkMiIsImlkcCI6IkxvY2FsQWNjb3VudCIsIm9pZCI6IjAzZjdlMGNkLWEzMDUtNGUyNC1hODA2LTE0MzhiZWI2ZmRhYiIsInN1YiI6IjAzZjdlMGNkLWEzMDUtNGUyNC1hODA2LTE0MzhiZWI2ZmRhYiIsIm5hbWUiOiJCQVNUSUFOIEhFUk5BTkRFWiBXSUxTT04iLCJlbWFpbHMiOlsiYmFzdGlhbi5oZXJuYW5kZXpAdGVucG8uY2wiXSwidGZwIjoiQjJDXzFfd2VicGF5IiwiYXpwIjoiM2IxYjcxMzMtMzg5Zi00ZTFiLTgxMTctMjdiN2JhZjZkZmQyIiwidmVyIjoiMS4wIiwiaWF0IjoxNTc3NzEyODI1fQ.sKtw2dfAFlHi1PW3jnKeKxbVQXNwqwJ_7ar_cDMs74ltOXHJ0jXytW3S-EObbyLYMgfIW1cnEw_xcyVq5dK1hXvr0nhLt2UEM4t7drrciPaOpMF5uNKNlW6u2zfb963RdPBcAAzRk3Zl11aE0PBrjEzG7RV1ZkX79QYp4H_-j0-kriQ42Pj05d8lK8-79aamSAgfiBGcjBUHPecYOwxvURCK76Zf6YvKRagIUylIQA7lJTP5VuVho6U4kJAOzjoEJqLLgvySiYuA5h6Jm3EDWRQfXUE_7vfkBuSmUwoYQZcEYkGJrcZOsZ8RhLrcIXsYZeudxeGhvFugF9zt3VYgpQ");
        when(loginRestClient.login(Mockito.any(LoginRequestDTO.class)))
                .thenReturn(tokenResponse);

        UserResponse userResponse = new UserResponse();
        userResponse.setId(UUID.randomUUID());
        userResponse.setState(UserStateType.ACTIVE);

        when(userRestClient.getUserByProvider(Mockito.any(UUID.class)))
                .thenReturn(Optional.of(userResponse));

        doThrow(new TenpoException(HttpStatus.NOT_FOUND,INVALID_PAN,"El PAN no corresponde al cliente"))
                .when(cardRestClient).checkIfCardBelongsToUser(Mockito.any(UUID.class),Mockito.anyString());

        try {
            customer2faService.login(CustomerLoginRequest
                    .builder()
                    .email("test@email.com")
                    .clave("1234")
                    .pan("5555XXXXX55555")
                    .build());
            Assert.fail("Can't be here");

        }catch (TenpoException e){
            Assert.assertEquals("HttpStatus debe ser igual", HttpStatus.NOT_FOUND, e.getCode());
            Assert.assertEquals("Codigo debe ser igual", INVALID_PAN, e.getErrorCode());
            throw e;
        }
    }

    @Test
    public void login() throws IOException, ParseException {
        LoginResponseDTO tokenResponse = new LoginResponseDTO();
        tokenResponse.setAccessToken("eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiIsImtpZCI6Ilg1ZVhrNHh5b2pORnVtMWtsMll0djhkbE5QNC1jNTdkTzZRR1RWQndhTmsifQ.eyJpc3MiOiJodHRwczovL3RlbnBvZGV2Mi5iMmNsb2dpbi5jb20vY2FlMDlmZWUtNDczZS00OGUyLTk5ZGMtM2I3YmY0YTk3NDExL3YyLjAvIiwiZXhwIjoxNTc3NzEzMTI1LCJuYmYiOjE1Nzc3MTI4MjUsImF1ZCI6IjNiMWI3MTMzLTM4OWYtNGUxYi04MTE3LTI3YjdiYWY2ZGZkMiIsImlkcCI6IkxvY2FsQWNjb3VudCIsIm9pZCI6IjAzZjdlMGNkLWEzMDUtNGUyNC1hODA2LTE0MzhiZWI2ZmRhYiIsInN1YiI6IjAzZjdlMGNkLWEzMDUtNGUyNC1hODA2LTE0MzhiZWI2ZmRhYiIsIm5hbWUiOiJCQVNUSUFOIEhFUk5BTkRFWiBXSUxTT04iLCJlbWFpbHMiOlsiYmFzdGlhbi5oZXJuYW5kZXpAdGVucG8uY2wiXSwidGZwIjoiQjJDXzFfd2VicGF5IiwiYXpwIjoiM2IxYjcxMzMtMzg5Zi00ZTFiLTgxMTctMjdiN2JhZjZkZmQyIiwidmVyIjoiMS4wIiwiaWF0IjoxNTc3NzEyODI1fQ.sKtw2dfAFlHi1PW3jnKeKxbVQXNwqwJ_7ar_cDMs74ltOXHJ0jXytW3S-EObbyLYMgfIW1cnEw_xcyVq5dK1hXvr0nhLt2UEM4t7drrciPaOpMF5uNKNlW6u2zfb963RdPBcAAzRk3Zl11aE0PBrjEzG7RV1ZkX79QYp4H_-j0-kriQ42Pj05d8lK8-79aamSAgfiBGcjBUHPecYOwxvURCK76Zf6YvKRagIUylIQA7lJTP5VuVho6U4kJAOzjoEJqLLgvySiYuA5h6Jm3EDWRQfXUE_7vfkBuSmUwoYQZcEYkGJrcZOsZ8RhLrcIXsYZeudxeGhvFugF9zt3VYgpQ");
        when(loginRestClient.login(Mockito.any(LoginRequestDTO.class)))
                .thenReturn(tokenResponse);

        UserResponse userResponse = new UserResponse();
        userResponse.setId(UUID.randomUUID());
        userResponse.setState(UserStateType.ACTIVE);

        when(userRestClient.getUserByProvider(Mockito.any(UUID.class)))
                .thenReturn(Optional.of(userResponse));

        doNothing().when(cardRestClient)
                .checkIfCardBelongsToUser(Mockito.any(UUID.class),Mockito.anyString());

        try {
            customer2faService.login(CustomerLoginRequest
                    .builder()
                    .email("test@email.com")
                    .clave("1234")
                    .pan("5555XXXXX55555")
                    .build());
        }catch (TenpoException e){
            Assert.fail("Can't be here");
        }
    }

}
