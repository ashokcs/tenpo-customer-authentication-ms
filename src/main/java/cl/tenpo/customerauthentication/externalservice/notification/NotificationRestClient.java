package cl.tenpo.customerauthentication.externalservice.notification;

import cl.tenpo.customerauthentication.externalservice.notification.dto.EmailV2Dto;
import cl.tenpo.customerauthentication.externalservice.notification.dto.TwoFactorPushRequest;

public interface NotificationRestClient {
    void sendMessagePush(TwoFactorPushRequest twoFactorPushRequest);
    void sendEmailv2(EmailV2Dto emailRequest);
    String getURINotificationAndTraceRequestBody(Object object, String urlEndpoint);
}
