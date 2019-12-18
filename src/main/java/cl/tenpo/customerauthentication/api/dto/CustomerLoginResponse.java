package cl.tenpo.customerauthentication.api.dto;

import cl.tenpo.customerauthentication.model.AmountAndCurrency;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CustomerLoginResponse {
    private String token;
    private String rut;
    private String email;
    private AmountAndCurrency balance;
}
