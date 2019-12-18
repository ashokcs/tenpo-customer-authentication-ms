package cl.tenpo.customerauthentication.dto;

import cl.tenpo.customerauthentication.model.ChallengeStatus;
import cl.tenpo.customerauthentication.model.ChallengeType;
import lombok.*;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CustomerChallengeDTO {

    private UUID id;
    private CustomerTransactionContextDTO customerTransaction;
    private ChallengeType challengeType;
    private String callbackUri;
    private ChallengeStatus status;
    private LocalDateTime created;
    private LocalDateTime updated;

}
