package cl.tenpo.customerauthentication.externalservice.verifier;

import cl.tenpo.customerauthentication.constants.ErrorCode;
import cl.tenpo.customerauthentication.exception.TenpoException;
import cl.tenpo.customerauthentication.externalservice.verifier.dto.GenerateTwoFactorResponse;
import cl.tenpo.customerauthentication.properties.CloudProps;
import cl.tenpo.customerauthentication.properties.UsersProps;
import cl.tenpo.customerauthentication.properties.VerifierProps;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
@Slf4j
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
            VerifierProps verifierProps = config.getVerifier();
            resourcePathBuilder.append(verifierProps.getGenerateTwoFactorResourcePath());

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
            throw new TenpoException(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.INTERNAL_ERROR);
        } catch (Exception e) {
            log.error("[generateTwoFactorCode] Exception:",e);
            throw new TenpoException(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.INTERNAL_ERROR);
        }
    }

    @Override
    public boolean validateTwoFactorCode(UUID userId, String code) {
        try {
            log.info("[validateTwoFactorCode] validating code for: [{}]", userId.toString());

            VerifierProps verifierProps = config.getVerifier();
            String resourcePath = verifierProps.getValidateTwoFactorCodeResourcePath();

            Map<String, String> map = new HashMap<>();
            map.put("userId", userId.toString());
            map.put("code", code);

            log.debug("[validateTwoFactorCode] URL [{}]", resourcePath);
            restTemplate.getForEntity(resourcePath, Void.class, map);
            return true;
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error(String.format("[validateTwoFactorCode] Error HTTP al pedir codigo 2 factores [%s]", e.getStatusCode()));
            return false;
        } catch (Exception e) {
            throw new TenpoException(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.INTERNAL_ERROR);
        }
    }
}
