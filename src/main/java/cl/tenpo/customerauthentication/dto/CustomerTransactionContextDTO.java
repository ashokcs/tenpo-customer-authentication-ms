package cl.tenpo.customerauthentication.dto;

import cl.tenpo.customerauthentication.model.CustomerTransactionStatus;
import lombok.*;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CustomerTransactionContextDTO {

    private UUID id;
    private UUID externalId;
    private String txType;
    private BigDecimal txAmount;
    private Integer txCurrency;
    private String txMerchant;
    private Integer txCountryCode;
    private String txPlaceName;
    private String txOther;
    private CustomerTransactionStatus status;
    private LocalDateTime created;
    private LocalDateTime updated;

}
