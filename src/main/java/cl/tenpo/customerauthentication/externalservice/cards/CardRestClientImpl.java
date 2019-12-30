package cl.tenpo.customerauthentication.externalservice.cards;

import cl.tenpo.customerauthentication.exception.TenpoException;
import cl.tenpo.customerauthentication.externalservice.cards.dto.CheckCardBelongsRequest;
import cl.tenpo.customerauthentication.properties.CloudProps;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

import static cl.tenpo.customerauthentication.constants.ErrorCode.INVALID_PAN;

@Component
@Slf4j
@ConditionalOnProperty(name = "krealo.cloud.cards.implement", havingValue = "real")
public class CardRestClientImpl implements CardRestClient{

    @Autowired
    private CloudProps config;

    @Autowired
    private RestTemplate restTemplate;

    @Override
    public void checkIfCardBelongsToUser(UUID userId, String truncatedPan) {
        try{
            log.info("[checkIfCardBelongsToUser] IN {}{}{}",userId,truncatedPan,CheckCardBelongsRequest
                    .builder()
                    .pan(truncatedPan)
                    .userUuid(userId)
                    .build());

            restTemplate.postForEntity(config.getCards().getCheckIfCardBelongsToUser(),
                    new HttpEntity<>(CheckCardBelongsRequest
                            .builder()
                            .pan(truncatedPan)
                            .userUuid(userId)
                            .build(), initHeader()), Void.class);

        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error("[checkIfCardBelongsToUser] User not found ",e);
            throw new TenpoException(HttpStatus.NOT_FOUND,INVALID_PAN,"El PAN no corresponde al cliente");
        } catch (Exception e) {
            log.error("[checkIfCardBelongsToUser] Exception:",e);
            throw new TenpoException(HttpStatus.NOT_FOUND,INVALID_PAN,"El PAN no corresponde al cliente");
        }
    }

    private HttpHeaders initHeader() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
}
