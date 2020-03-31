package cl.tenpo.customerauthentication.externalservice.cards;

import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.web.client.HttpServerErrorException;

import java.util.UUID;

public interface CardRestClient {

    @Retryable(value = {HttpServerErrorException.class}, maxAttempts = 3, backoff = @Backoff(delay = 5000))
    void checkIfCardBelongsToUser(UUID userId, String truncatedPan);

}