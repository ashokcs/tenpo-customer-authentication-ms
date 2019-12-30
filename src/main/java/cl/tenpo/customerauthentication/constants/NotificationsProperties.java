package cl.tenpo.customerauthentication.constants;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "constants.notifications")
public class NotificationsProperties {
    private String twoFactorMailFrom;
    private String twoFactorMailSubject;
    private String twoFactorMailTemplate;
}
