package cl.tenpo.customerauthentication.externalservice.cards.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.UUID;

@Builder
@Getter
@Setter
@ToString
public class CheckCardBelongsRequest {

    @JsonProperty("userUuid")
    private UUID userUuid;

    private String pan;
}
