package cl.tenpo.customerauthentication.externalservice.notification;

import cl.tenpo.customerauthentication.constants.ErrorCode;
import cl.tenpo.customerauthentication.exception.TenpoException;
import cl.tenpo.customerauthentication.externalservice.notification.dto.EmailDto;
import cl.tenpo.customerauthentication.externalservice.notification.dto.TwoFactorPushRequest;
import cl.tenpo.customerauthentication.properties.CloudProps;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

@Component
@AllArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "krealo.cloud.notifications.implement", havingValue = "real")
public class NotificationRestClientImpl implements NotificationRestClient {

    @Autowired
    private CloudProps config;

    @Autowired
    private RestTemplate restTemplate;

    @Async("taskExecutor")
    @Override
    public void sendMessagePush(TwoFactorPushRequest twoFactorPushRequest) {
        try {
            restTemplate.postForEntity(
                    getURINotificationAndTraceRequestBody(
                            twoFactorPushRequest,
                        config.getNotifications().getSendNotificationResourcePath()),
                        new HttpEntity<>(twoFactorPushRequest,
                        initHeader()),
                        Void.class);

        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error("HttpClientErrorException : {}", e.getMessage());
            throw new TenpoException(HttpStatus.UNPROCESSABLE_ENTITY, ErrorCode.PUSH_NOTIFICATION_ERROR);
        }
    }

    @Override
    public void sendEmail(EmailDto emailRequest){
        try {
            getURINotificationAndTraceRequestBody(emailRequest, config.getNotifications().getSendGenericEmailResourcePath());
            String urlEndpoint = config.getNotifications().getSendGenericEmailResourcePath();
            restTemplate.postForEntity(urlEndpoint, new HttpEntity<>(emailRequest, initHeader()), Void.class);
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error("HttpClientErrorException : {}", e.getMessage());
            throw new TenpoException(HttpStatus.UNPROCESSABLE_ENTITY, ErrorCode.ERROR_EMAIL_NOTIFICATION);
        }
    }

    @Override
    public String getURINotificationAndTraceRequestBody(Object object, String urlEndpoint) {
        try {
            log.debug("request uri notification {} with data {}", urlEndpoint, object);
        } catch (Exception e) {
            log.error(String.format("Error parsing request %s from  uri %s ", object, urlEndpoint), e);
            throw new TenpoException(HttpStatus.UNPROCESSABLE_ENTITY, ErrorCode.ERROR_EMAIL_NOTIFICATION);
        }
        return urlEndpoint;
    }

    private HttpHeaders initHeader() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }


}
