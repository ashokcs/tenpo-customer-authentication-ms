package cl.tenpo.customerauthentication.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author Daniel Neciosup <daniel.neciosup@avantica.net>
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "cloud.azure")
public class AzureProperties {
    private String tenantName;
    private String clientId;
    private String clientSecret;
    private String blobStorageDirectory;
    private String blobStorageContainerName;
    private String version;
}
