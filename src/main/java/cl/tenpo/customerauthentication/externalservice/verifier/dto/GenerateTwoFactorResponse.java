package cl.tenpo.customerauthentication.externalservice.verifier.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.UUID;

@Getter
@Setter
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class GenerateTwoFactorResponse {
    private UUID id;
    private String generatedCode;
}
