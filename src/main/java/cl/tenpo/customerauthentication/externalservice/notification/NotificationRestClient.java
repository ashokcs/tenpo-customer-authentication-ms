package cl.tenpo.customerauthentication.externalservice.notification;

import cl.tenpo.customerauthentication.externalservice.notification.dto.EmailDto;
import cl.tenpo.customerauthentication.externalservice.notification.dto.TwoFactorPushRequest;

public interface NotificationRestClient {
    void sendMessagePush(TwoFactorPushRequest twoFactorPushRequest);
    void sendEmail(EmailDto emailRequest);
    String getURINotificationAndTraceRequestBody(Object object, String urlEndpoint);
}
