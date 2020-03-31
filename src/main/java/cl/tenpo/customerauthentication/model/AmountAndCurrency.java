package cl.tenpo.customerauthentication.model;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AmountAndCurrency {
    private BigDecimal value;
    private Integer currencyCode;
}
