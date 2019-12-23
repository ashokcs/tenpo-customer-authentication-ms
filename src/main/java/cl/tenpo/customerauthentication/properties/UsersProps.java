package cl.tenpo.customerauthentication.properties;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class UsersProps {

    private String filterUserResourcePath;
    private String getUserByIdResourcePath;
    private String getUserByRutPath;
}
