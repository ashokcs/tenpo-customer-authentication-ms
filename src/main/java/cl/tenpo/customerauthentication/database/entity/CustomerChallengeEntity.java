package cl.tenpo.customerauthentication.database.entity;

import cl.tenpo.customerauthentication.model.ChallengeStatus;
import cl.tenpo.customerauthentication.model.ChallengeType;
import lombok.Data;
import lombok.ToString;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
@ToString
@Table(name = "customer_challenge")
public class CustomerChallengeEntity {

    @Id
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "transaction_context_id")
    private CustomerTransactionContextEntity customerTransaction;

    @Enumerated(EnumType.STRING)
    private ChallengeType challengeType;

    private String callbackUri;

    @Enumerated(EnumType.STRING)
    private ChallengeStatus status;

    private LocalDateTime created;
    private LocalDateTime updated;

}
