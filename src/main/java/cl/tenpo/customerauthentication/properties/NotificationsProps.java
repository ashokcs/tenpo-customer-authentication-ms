package cl.tenpo.customerauthentication.properties;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class NotificationsProps {

    private String baseUrl;
    private String sendNotificationResourcePath;
    private String sendEmailNotificationResourcePath;
    private String sendCreateNotificationSettingResourcePath;
    private String sendGenericEmailResourcePath;

}
