package cl.tenpo.customerauthentication.externalservice.azure.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

/**
 * @author Daniel Neciosup <daniel.neciosup@avantica.net>
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(value = JsonInclude.Include.NON_NULL)
public class PasswordProfileModel {
    private Boolean forceChangePasswordNextLogin;
    private String password;
}
