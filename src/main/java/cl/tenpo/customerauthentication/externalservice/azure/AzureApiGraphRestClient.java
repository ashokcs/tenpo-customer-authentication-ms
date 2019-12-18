package cl.tenpo.customerauthentication.externalservice.azure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import pe.krealo.architecture.exception.MineException;
import pe.krealo.mine.identityprovider.properties.AzureApiGraphProperties;
import pe.krealo.mine.identityprovider.properties.AzureProperties;
import pe.krealo.mine.identityprovider.thirdparty.external.azure.dto.ResetRequest;
import pe.krealo.mine.identityprovider.thirdparty.external.azure.dto.UserRequest;
import pe.krealo.mine.identityprovider.thirdparty.external.azure.dto.UserResponse;

import java.util.Arrays;

import static org.springframework.http.HttpStatus.CONFLICT;
import static pe.krealo.architecture.util.JsonUtils.objectToJSONString;
import static pe.krealo.mine.identityprovider.constant.ErrorCode.*;

/**
 * @author Daniel Neciosup <daniel.neciosup@avantica.net>
 */
@Slf4j
@Component
@AllArgsConstructor
public class AzureApiGraphRestClient {

    private static final String BEARER_TOKEN_FORMAT = "Bearer %s";

    private final AzureProperties azureProperties;
    private final AzureApiGraphProperties azureApiGraphProperties;
    private final RestTemplate restTemplate;

    public UserResponse createUser(String accessToken, UserRequest request) {
        ResponseEntity<UserResponse> response;
        try {
            log.debug("User for creation in azure: {}", objectToJSONString(request));
            response = restTemplate.exchange(
                    azureApiGraphProperties.getUserMaintenanceResourcePath()
                            + "?api-version="
                            + azureProperties.getVersion(),
                    HttpMethod.POST,
                    new HttpEntity<>(request, getHeaders(accessToken)),
                    UserResponse.class
            );
        } catch (HttpClientErrorException | HttpServerErrorException exception) {
            log.error("[Error during azure account creation]", exception);
            throw new MineException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    AZURE_USER_CREATION_ERROR_CODE
            );
        }
        return response.getBody();
    }

    public void updateUser(String accessToken, String id, UserRequest request) {
        restTemplate.exchange(
                String.format("%s/%s?api-version=%s",
                        azureApiGraphProperties.getUserMaintenanceResourcePath(),
                        id,
                        azureProperties.getVersion()),
                HttpMethod.PATCH,
                new HttpEntity<>(request, getHeaders(accessToken)),
                UserResponse.class
        );
    }


    public void changePassword(String accessToken, ResetRequest request) {
        try {
            String adUrl = String.format("%s/%s?api-version=%s", azureApiGraphProperties.getResetPasswordResourcePath(),
                    request.getId(),
                    azureProperties.getVersion());
            log.info("Start changePassword in AD. url: " + adUrl);
            restTemplate.exchange(
                    adUrl,
                    HttpMethod.PATCH,
                    new HttpEntity<>(objectToJson(request.getResetPassword()), getHeaders(accessToken)),
                    Void.class
            );
            log.info("Success changePassword in AD");
        } catch (HttpClientErrorException | HttpServerErrorException exception) {
            log.error("[Exception in changePassword]: " + exception.getMessage(), exception);
            throw new MineException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    CHANGE_PASSWORD_ERROR_CODE,
                    exception.getMessage()
            );
        }
    }

    public void resetPassword(String accessToken, ResetRequest request) {
        try {
            restTemplate.setRequestFactory(getFactory());
            restTemplate.exchange(
                    String.format("%s/%s?api-version=%s", azureApiGraphProperties.getResetPasswordResourcePath(),
                            request.getId(),
                            azureProperties.getVersion()),
                    HttpMethod.PATCH,
                    new HttpEntity<>(objectToJson(request.getResetPassword()), getHeaders(accessToken)),
                    Object.class
            );
        } catch (HttpClientErrorException | HttpServerErrorException exception) {
            log.error("[FAIL] - [AzureApiGraphRestClient::resetPassword] - ["
                    + request.getId()
                    + "]: " + exception.getMessage());
            throw new MineException(HttpStatus.UNPROCESSABLE_ENTITY, CHANGE_PASSWORD_ERROR_CODE);
        }
    }

    private HttpHeaders getHeaders(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));

        return headers;
    }

    private HttpComponentsClientHttpRequestFactory getFactory() {
        final int TIMEOUT = 3600;
        HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
        requestFactory.setConnectTimeout(TIMEOUT);
        requestFactory.setReadTimeout(TIMEOUT);
        return requestFactory;
    }

    private String objectToJson(Object object) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new MineException(CONFLICT, JSON_CONVERSION_PROBLEM);
        }
    }

}
