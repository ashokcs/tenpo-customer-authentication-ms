package cl.tenpo.customerauthentication.properties;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class AccountsProps {

    private String baseUrl;
    private String getAccountByUserResourcePath;
    private String getAccountResourcePath;
    private String updateAccountBalanceResourcePath;
}
