package cl.tenpo.customerauthentication.properties;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class LoginProps {
    private String getLoginPath;
    private String getApiKey;
}
