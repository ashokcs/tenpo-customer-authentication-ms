package cl.tenpo.customerauthentication.api.dto;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ValidateChallengeResponse {

    private String externalId;
    private String result;

}
