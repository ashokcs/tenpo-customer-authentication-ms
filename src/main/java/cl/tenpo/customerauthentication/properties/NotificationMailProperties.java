package cl.tenpo.customerauthentication.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "constants.notifications")
public class NotificationMailProperties {
    private String twoFactorMailFrom;
    private String twoFactorMailSubject;
    private String twoFactorMailTemplate;
}
