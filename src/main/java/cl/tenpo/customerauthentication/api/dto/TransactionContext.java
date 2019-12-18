package cl.tenpo.customerauthentication.api.dto;

import cl.tenpo.customerauthentication.model.AmountAndCurrency;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TransactionContext {
    private String txType;
    private AmountAndCurrency txAmount;
    private String txMerchant;
    private Integer txCountryCode;
    private String txPlaceName;
    private String txOther;
}
