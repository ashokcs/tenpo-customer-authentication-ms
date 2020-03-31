package cl.tenpo.customerauthentication.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "constants.transaction-context")
public class CustomerTransactionContextProperties {
    private Integer expirationTimeInMinutes;
    private Integer passwordAttempts;
    private Integer challengeReuseTimeInSeconds;
}
