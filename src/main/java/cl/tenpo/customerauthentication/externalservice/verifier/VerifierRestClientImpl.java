package cl.tenpo.customerauthentication.externalservice.verifier;

import cl.tenpo.customerauthentication.externalservice.verifier.dto.GenerateTwoFactorResponse;
import cl.tenpo.customerauthentication.properties.CloudProps;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
@Slf4j
@ConditionalOnProperty(name = "krealo.cloud.verifier.implement", havingValue = "real")
public class VerifierRestClientImpl implements VerifierRestClient {

    @Autowired
    private CloudProps config;

    @Autowired
    private RestTemplate restTemplate;

    @Override
    public GenerateTwoFactorResponse generateTwoFactorCode(UUID userId, UUID codeId, Long expiration) {
        try {
            log.info("[generateTwoFactorCode] generate code for: [{}]", userId.toString());

            StringBuilder resourcePathBuilder = new StringBuilder();
            resourcePathBuilder.append(config.getUsers().getGetUserByIdResourcePath());

            if (codeId != null) {
                resourcePathBuilder.append(String.format("?id=%s", codeId));
            }

            if (expiration != null) {
                resourcePathBuilder.append(codeId != null ? "&" : "?");
                resourcePathBuilder.append(String.format("expiration=%s", expiration));
            }

            String resourcePath = resourcePathBuilder.toString();

            Map<String, String> map = new HashMap<>();
            map.put("userId", userId.toString());
            log.debug("[generateTwoFactorCode] URL [{}]", resourcePath);
            ResponseEntity<GenerateTwoFactorResponse> response = restTemplate.postForEntity(resourcePath, null, GenerateTwoFactorResponse.class, map);

            log.debug("[generateTwoFactorCode] Response: [{}]", response);
            return response.getBody();
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error("[generateTwoFactorCode] Error HTTP al pedir codigo 2 factores ",e);
            throw (e);
        } catch (Exception e) {
            log.error("[generateTwoFactorCode] Exception:",e);
            throw (e);
        }
    }
}
