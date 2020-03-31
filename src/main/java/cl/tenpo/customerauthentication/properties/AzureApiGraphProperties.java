package cl.tenpo.customerauthentication.properties;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author Daniel Neciosup <daniel.neciosup@avantica.net>
 */
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Component
@ConfigurationProperties(prefix = "cloud.azure.api-graph")
public class AzureApiGraphProperties {
    private String getAccessTokenResourcePath;
    private String getLoginAccessTokenResourcePath;
    private String userMaintenanceResourcePath;
    private String changePasswordResourcePath;
    private String resetPasswordResourcePath;
}
