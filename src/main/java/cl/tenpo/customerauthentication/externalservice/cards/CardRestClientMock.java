package cl.tenpo.customerauthentication.externalservice.cards;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Slf4j
@ConditionalOnProperty(name = "krealo.cloud.users.implement", havingValue = "mock")
public class CardRestClientMock implements CardRestClient {

    @Override
    public void checkIfCardBelongsToUser(UUID userId, String truncatedPan) {

    }
}
