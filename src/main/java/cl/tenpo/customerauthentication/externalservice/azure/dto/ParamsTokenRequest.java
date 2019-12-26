package cl.tenpo.customerauthentication.externalservice.azure.dto;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ParamsTokenRequest {
    private String grantType;
    private String username;
    private String password;
}
