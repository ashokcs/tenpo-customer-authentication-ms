package cl.tenpo.customerauthentication.api.dto;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AbortChallengeResponse {
    private String externalId;
    private String result;
}
