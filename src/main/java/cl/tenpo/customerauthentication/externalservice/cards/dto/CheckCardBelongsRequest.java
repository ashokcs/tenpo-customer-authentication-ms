package cl.tenpo.customerauthentication.externalservice.cards.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.util.UUID;

@Builder
public class CheckCardBelongsRequest {

    @JsonProperty("userUuid")
    private UUID userUuid;

    private String pan;
}
