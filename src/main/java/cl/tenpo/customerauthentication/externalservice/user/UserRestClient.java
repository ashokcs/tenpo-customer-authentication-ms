package cl.tenpo.customerauthentication.externalservice.user;

import cl.tenpo.customerauthentication.externalservice.user.dto.UserResponse;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

public interface UserRestClient {

    @Retryable(value = { HttpClientErrorException.class, HttpServerErrorException.class }, maxAttempts = 3, backoff = @Backoff(delay = 5000))
    Optional<UserResponse> getUser(UUID userId) throws IOException;

}
