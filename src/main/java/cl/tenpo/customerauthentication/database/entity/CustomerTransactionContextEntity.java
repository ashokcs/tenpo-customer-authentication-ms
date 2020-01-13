package cl.tenpo.customerauthentication.database.entity;

import cl.tenpo.customerauthentication.model.CustomerTransactionStatus;
import lombok.Data;
import lombok.ToString;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Entity
@ToString
@Table(name = "customer_transaction_context")
public class CustomerTransactionContextEntity {

    @Id
    private UUID id;
    private UUID userId;
    private String externalId;
    private String txType;
    private BigDecimal txAmount;
    private Integer txCurrency;
    private String txMerchant;
    private Integer txCountryCode;
    private String txPlaceName;
    private String txOther;
    @Enumerated(EnumType.STRING)
    private CustomerTransactionStatus status;
    private LocalDateTime created;
    private LocalDateTime updated;
    private Integer attempts;
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "customerTransaction")
    private List<CustomerChallengeEntity> challenge;

}
