package cl.tenpo.customerauthentication.api.dto;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AbortChallengeRequest {
    private String externalId;
}
