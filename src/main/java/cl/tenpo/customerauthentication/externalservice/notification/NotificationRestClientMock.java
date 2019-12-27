package cl.tenpo.customerauthentication.externalservice.notification;

import cl.tenpo.customerauthentication.externalservice.notification.dto.EmailDto;
import cl.tenpo.customerauthentication.externalservice.notification.dto.TwoFactorPushRequest;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;


@Component
@AllArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "krealo.cloud.notifications.implement", havingValue = "mock")
public class NotificationRestClientMock implements NotificationRestClient {

    @Override
    public void sendMessagePush(TwoFactorPushRequest twoFactorPushRequest) {
        log.info("[sendMessagePush] Mock Send Push OK");
    }

    @Override
    public void sendEmail(EmailDto emailRequest) {
        log.info("[sendEmail] Mock Send Email OK");
    }

    @Override
    public String getURINotificationAndTraceRequestBody(Object object, String urlEndpoint) {
        return null;
    }
}
