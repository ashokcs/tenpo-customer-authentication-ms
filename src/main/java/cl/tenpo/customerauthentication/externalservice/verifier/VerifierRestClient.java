package cl.tenpo.customerauthentication.externalservice.verifier;

import cl.tenpo.customerauthentication.externalservice.verifier.dto.GenerateTwoFactorResponse;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import java.util.UUID;

public interface VerifierRestClient {

    @Retryable(value = { HttpClientErrorException.class, HttpServerErrorException.class }, maxAttempts = 3, backoff = @Backoff(delay = 5000))
    GenerateTwoFactorResponse generateTwoFactorCode(UUID userId, UUID codeId, Long expiration);

}
