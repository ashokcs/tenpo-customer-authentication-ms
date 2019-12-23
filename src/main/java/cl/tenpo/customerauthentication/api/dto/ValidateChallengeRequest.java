package cl.tenpo.customerauthentication.api.dto;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ValidateChallengeRequest {
    private String externalId;
    private String response;
}
