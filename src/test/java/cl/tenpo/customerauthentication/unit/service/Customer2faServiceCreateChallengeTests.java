package cl.tenpo.customerauthentication.unit.service;

import cl.tenpo.customerauthentication.CustomerAuthenticationMsApplicationTests;
import cl.tenpo.customerauthentication.api.dto.CreateChallengeRequest;
import cl.tenpo.customerauthentication.constants.CustomerAuthenticationConstants;
import cl.tenpo.customerauthentication.constants.ErrorCode;
import cl.tenpo.customerauthentication.exception.TenpoException;
import cl.tenpo.customerauthentication.externalservice.notification.NotificationRestClient;
import cl.tenpo.customerauthentication.externalservice.notification.dto.EmailV2Dto;
import cl.tenpo.customerauthentication.externalservice.notification.dto.MessageType;
import cl.tenpo.customerauthentication.externalservice.notification.dto.NotificationEventType;
import cl.tenpo.customerauthentication.externalservice.notification.dto.TwoFactorPushRequest;
import cl.tenpo.customerauthentication.externalservice.user.UserRestClient;
import cl.tenpo.customerauthentication.externalservice.user.dto.UserResponse;
import cl.tenpo.customerauthentication.externalservice.user.dto.UserStateType;
import cl.tenpo.customerauthentication.model.ChallengeType;
import cl.tenpo.customerauthentication.model.NewCustomerChallenge;
import cl.tenpo.customerauthentication.service.Customer2faService;
import cl.tenpo.customerauthentication.service.CustomerChallengeService;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.*;

@RunWith(SpringRunner.class)
@SpringBootTest
public class Customer2faServiceCreateChallengeTests extends CustomerAuthenticationMsApplicationTests {

    @Autowired
    Customer2faService customer2faService;

    @MockBean
    UserRestClient userRestClient;

    @MockBean
    CustomerChallengeService customerChallengeService;

    @MockBean
    NotificationRestClient notificationRestClient;

    @Test
    public void createChallenge_sendEmail() throws IOException {
        UUID userId = UUID.randomUUID();
        CreateChallengeRequest createChallengeRequest = randomChallengeRequest();
        createChallengeRequest.setChallengeType(ChallengeType.OTP_MAIL);

        UserResponse userResponse = new UserResponse();
        userResponse.setId(userId);
        userResponse.setState(UserStateType.ACTIVE);
        userResponse.setEmail("user@mail.com");
        when(userRestClient.getUser(userId))
                .thenReturn(Optional.of(userResponse));

        NewCustomerChallenge newCustomerChallenge = new NewCustomerChallenge();
        newCustomerChallenge.setChallengeId(UUID.randomUUID());
        newCustomerChallenge.setChallengeType(createChallengeRequest.getChallengeType());
        newCustomerChallenge.setCode("123321");
        when(customerChallengeService.createRequestedChallenge(userId, createChallengeRequest))
                .thenReturn(newCustomerChallenge);

        doNothing().when(notificationRestClient).sendEmailv2(any());

        customer2faService.createChallenge(userId, createChallengeRequest);

        // Debe llamar al servicio de email
        ArgumentCaptor<EmailV2Dto> mailCaptor = ArgumentCaptor.forClass(EmailV2Dto.class);
        verify(notificationRestClient, times(1)).sendEmailv2(mailCaptor.capture());
        Assert.assertEquals("From twofactor mail", CustomerAuthenticationConstants.TWO_FACTOR_MAIL_FROM, mailCaptor.getValue().getFrom());
        Assert.assertEquals("To user mail", userResponse.getEmail(), mailCaptor.getValue().getTo());
        Assert.assertEquals("ReferenceId del challenge", newCustomerChallenge.getChallengeId().toString(), mailCaptor.getValue().getReferenceId());
        Assert.assertEquals("TwoFactor subject", CustomerAuthenticationConstants.TWO_FACTOR_MAIL_SUBJECT, mailCaptor.getValue().getSubject());
        Assert.assertEquals("template two factor", CustomerAuthenticationConstants.TWO_FACTOR_MAIL_TEMPLATE, mailCaptor.getValue().getTemplate());

        Map<String, String> mailMap = mailCaptor.getValue().getParams();
        Assert.assertEquals("Debe enviarse con el nombre", userResponse.getFirstName(), mailMap.get("{{name}}"));
        Assert.assertEquals("Debe enviarse con el fullName", String.format("%s %s ", userResponse.getFirstName(), userResponse.getLastName()), mailMap.get("{{fullName}}"));
        Assert.assertEquals("Debe enviarse con el transactionId como challengeId", newCustomerChallenge.getChallengeId().toString(), mailMap.get("{{transactionId}}"));
        Assert.assertEquals("Debe enviarse con el codigo", newCustomerChallenge.getCode(), mailMap.get("{{code}}"));
    }

    @Test
    public void createChallenge_sendSMS() throws IOException {
        UUID userId = UUID.randomUUID();
        CreateChallengeRequest createChallengeRequest = randomChallengeRequest();
        createChallengeRequest.setChallengeType(ChallengeType.OTP_SMS);

        UserResponse userResponse = new UserResponse();
        userResponse.setId(userId);
        userResponse.setState(UserStateType.ACTIVE);
        userResponse.setEmail("user@mail.com");
        when(userRestClient.getUser(userId))
                .thenReturn(Optional.of(userResponse));

        NewCustomerChallenge newCustomerChallenge = new NewCustomerChallenge();
        newCustomerChallenge.setChallengeId(UUID.randomUUID());
        newCustomerChallenge.setChallengeType(createChallengeRequest.getChallengeType());
        newCustomerChallenge.setCode("123321");
        when(customerChallengeService.createRequestedChallenge(userId, createChallengeRequest))
                .thenReturn(newCustomerChallenge);

        doNothing().when(notificationRestClient).sendEmailv2(any());

        customer2faService.createChallenge(userId, createChallengeRequest);

        // Debe llamar al servicio de email
        ArgumentCaptor<TwoFactorPushRequest> smsCaptor = ArgumentCaptor.forClass(TwoFactorPushRequest.class);
        verify(notificationRestClient, times(1)).sendMessagePush(smsCaptor.capture());
        Assert.assertEquals("Debe tener el userId", userId, smsCaptor.getValue().getUserId());
        Assert.assertEquals("Debe tener el challengeId como linkId", newCustomerChallenge.getChallengeId(), smsCaptor.getValue().getLinkId());
        Assert.assertEquals("Debe ser evento VERIFICATION_CODE", NotificationEventType.VERIFICATION_CODE, smsCaptor.getValue().getPusherEvent());
        Assert.assertEquals("Debe ser tipo SMS", MessageType.SMS, smsCaptor.getValue().getMessageType());
        Assert.assertEquals("Debe tener el code generado", newCustomerChallenge.getCode(), smsCaptor.getValue().getVerificationCode());
    }

    @Test
    public void createChallenge_sendPush() throws IOException {
        UUID userId = UUID.randomUUID();
        CreateChallengeRequest createChallengeRequest = randomChallengeRequest();
        createChallengeRequest.setChallengeType(ChallengeType.OTP_PUSH);

        UserResponse userResponse = new UserResponse();
        userResponse.setId(userId);
        userResponse.setState(UserStateType.ACTIVE);
        userResponse.setEmail("user@mail.com");
        when(userRestClient.getUser(userId))
                .thenReturn(Optional.of(userResponse));

        NewCustomerChallenge newCustomerChallenge = new NewCustomerChallenge();
        newCustomerChallenge.setChallengeId(UUID.randomUUID());
        newCustomerChallenge.setChallengeType(createChallengeRequest.getChallengeType());
        newCustomerChallenge.setCode("123321");
        when(customerChallengeService.createRequestedChallenge(userId, createChallengeRequest))
                .thenReturn(newCustomerChallenge);

        doNothing().when(notificationRestClient).sendEmailv2(any());

        customer2faService.createChallenge(userId, createChallengeRequest);

        // Debe llamar al servicio de email
        ArgumentCaptor<TwoFactorPushRequest> smsCaptor = ArgumentCaptor.forClass(TwoFactorPushRequest.class);
        verify(notificationRestClient, times(1)).sendMessagePush(smsCaptor.capture());
        Assert.assertEquals("Debe tener el userId", userId, smsCaptor.getValue().getUserId());
        Assert.assertEquals("Debe tener el challengeId como linkId", newCustomerChallenge.getChallengeId(), smsCaptor.getValue().getLinkId());
        Assert.assertEquals("Debe ser evento VERIFICATION_CODE", NotificationEventType.VERIFICATION_CODE, smsCaptor.getValue().getPusherEvent());
        Assert.assertEquals("Debe ser tipo PUSH", MessageType.PUSH_NOTIFICATION, smsCaptor.getValue().getMessageType());
        Assert.assertEquals("Debe tener el code generado", newCustomerChallenge.getCode(), smsCaptor.getValue().getVerificationCode());
    }

    @Test
    public void createChallenge_userNotFound() throws IOException {
        UUID userId = UUID.randomUUID();
        CreateChallengeRequest createChallengeRequest = randomChallengeRequest();
        createChallengeRequest.setChallengeType(ChallengeType.OTP_PUSH);

        when(userRestClient.getUser(userId))
                .thenReturn(Optional.empty());

        try {
            customer2faService.createChallenge(userId, createChallengeRequest);
            Assert.fail("Debe tirar una excepcion tenpo");
        } catch (TenpoException te) {
            Assert.assertEquals("Debe tener codigo 422", HttpStatus.UNPROCESSABLE_ENTITY, te.getCode());
            Assert.assertEquals("Debe tener codigo user not found", ErrorCode.USER_NOT_FOUND_OR_LOCKED, te.getErrorCode());
        } catch (Exception e) {
            Assert.fail("Debe tirar una excepcion tenpo");
        }
    }

    @Test
    public void createChallenge_userNotActive() throws IOException {
        UUID userId = UUID.randomUUID();
        CreateChallengeRequest createChallengeRequest = randomChallengeRequest();
        createChallengeRequest.setChallengeType(ChallengeType.OTP_PUSH);

        UserResponse userResponse = new UserResponse();
        userResponse.setId(userId);
        userResponse.setState(UserStateType.BLOCKED);
        userResponse.setEmail("user@mail.com");
        when(userRestClient.getUser(userId))
                .thenReturn(Optional.of(userResponse));

        try {
            customer2faService.createChallenge(userId, createChallengeRequest);
            Assert.fail("Debe tirar una excepcion tenpo");
        } catch (TenpoException te) {
            Assert.assertEquals("Debe tener codigo 422", HttpStatus.UNPROCESSABLE_ENTITY, te.getCode());
            Assert.assertEquals("Debe tener codigo user invalid", ErrorCode.USER_NOT_FOUND_OR_LOCKED, te.getErrorCode());
        } catch (Exception e) {
            Assert.fail("Debe tirar una excepcion tenpo");
        }
    }

    @Test
    public void createChallenge_errorGettingUser() throws IOException {
        UUID userId = UUID.randomUUID();
        CreateChallengeRequest createChallengeRequest = randomChallengeRequest();
        createChallengeRequest.setChallengeType(ChallengeType.OTP_PUSH);

        when(userRestClient.getUser(userId))
                .thenThrow(new NullPointerException()); // Cualquier excepcion

        try {
            customer2faService.createChallenge(userId, createChallengeRequest);
            Assert.fail("Debe tirar una excepcion tenpo");
        } catch (TenpoException te) {
            Assert.assertEquals("Debe tener codigo 422", HttpStatus.UNPROCESSABLE_ENTITY, te.getCode());
            Assert.assertEquals("Debe tener codigo user not found", ErrorCode.USER_NOT_FOUND_OR_LOCKED, te.getErrorCode());
        } catch (Exception e) {
            Assert.fail("Debe tirar una excepcion tenpo");
        }
    }
}
