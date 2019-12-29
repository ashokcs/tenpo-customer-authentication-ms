package cl.tenpo.customerauthentication.api.dto;

import cl.tenpo.customerauthentication.model.ChallengeResult;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ValidateChallengeResponse {

    private UUID externalId;
    private ChallengeResult result;

}
