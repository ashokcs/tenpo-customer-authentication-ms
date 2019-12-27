package cl.tenpo.customerauthentication.externalservice.verifier.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.UUID;

@Getter
@Setter
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)

public class GenerateTwoFactorResponse {
    @JsonProperty("id")
    private UUID id;

    @JsonProperty("generatedCode")
    private String generatedCode;
}
