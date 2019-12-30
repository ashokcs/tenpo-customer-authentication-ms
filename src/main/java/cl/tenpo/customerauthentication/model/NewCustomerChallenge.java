package cl.tenpo.customerauthentication.model;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class NewCustomerChallenge {
    UUID challengeId;
    String code;
    ChallengeType challengeType;
}
