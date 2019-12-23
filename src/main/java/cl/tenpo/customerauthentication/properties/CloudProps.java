package cl.tenpo.customerauthentication.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "krealo.cloud")
public class CloudProps {
    private AccountsProps accounts;
    private NotificationsProps notifications;
    private UsersProps users;
}
