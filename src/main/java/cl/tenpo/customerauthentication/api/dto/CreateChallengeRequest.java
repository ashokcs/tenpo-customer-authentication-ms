package cl.tenpo.customerauthentication.api.dto;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CreateChallengeRequest {
    private UUID externalId;
    private String challengeType;
    private TransactionContext transactionContext;
    private String callbackUri;
}
