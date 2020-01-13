package cl.tenpo.customerauthentication.api.dto;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ValidateChallengeRequest {
    private String externalId;
    private String response;
}
