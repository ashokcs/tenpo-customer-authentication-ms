package cl.tenpo.customerauthentication.externalservice.verifier;

import cl.tenpo.customerauthentication.externalservice.verifier.dto.GenerateTwoFactorResponse;
import cl.tenpo.customerauthentication.model.NewCustomerChallenge;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

@Component
@Slf4j
@ConditionalOnProperty(name = "krealo.cloud.verifier.implement", havingValue = "mock")
public class VerifierRestClientMock implements VerifierRestClient {
    @Override
    public GenerateTwoFactorResponse generateTwoFactorCode(UUID userId, UUID codeId, Long expiration) {
        GenerateTwoFactorResponse generateTwoFactorResponse = new GenerateTwoFactorResponse();
        generateTwoFactorResponse.setGeneratedCode("123321");
        generateTwoFactorResponse.setId(UUID.fromString("123e4567-e89b-12d3-a456-426655440000"));
        System.out.println("[generateTwoFactorCode] Mock llamado");
        return generateTwoFactorResponse;
    }
}
