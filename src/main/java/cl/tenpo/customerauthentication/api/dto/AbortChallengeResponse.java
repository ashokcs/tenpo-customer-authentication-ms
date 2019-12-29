package cl.tenpo.customerauthentication.api.dto;

import cl.tenpo.customerauthentication.model.CustomerTransactionStatus;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AbortChallengeResponse {
    private UUID externalId;
    private CustomerTransactionStatus result;
}
