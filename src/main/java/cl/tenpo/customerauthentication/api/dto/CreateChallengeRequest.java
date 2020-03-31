package cl.tenpo.customerauthentication.api.dto;

import cl.tenpo.customerauthentication.model.ChallengeType;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CreateChallengeRequest {
    private String externalId;
    private ChallengeType challengeType;
    private TransactionContext transactionContext;
    private String callbackUri;
}
