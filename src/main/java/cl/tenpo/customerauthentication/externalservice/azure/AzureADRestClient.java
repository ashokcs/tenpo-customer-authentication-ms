package cl.tenpo.customerauthentication.externalservice.azure;

import cl.tenpo.customerauthentication.exception.TenpoException;
import cl.tenpo.customerauthentication.externalservice.azure.dto.ParamsTokenRequest;
import cl.tenpo.customerauthentication.externalservice.azure.dto.TokenResponse;
import cl.tenpo.customerauthentication.properties.AzureApiGraphProperties;
import cl.tenpo.customerauthentication.properties.AzureProperties;
import lombok.AllArgsConstructor;
import org.apache.tomcat.util.codec.binary.Base64;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import java.util.Optional;


/**
 * @author Daniel Neciosup <daniel.neciosup@avantica.net>
 */
@Component
@AllArgsConstructor
public class AzureADRestClient {
    private static final String BASIC_TOKEN_FORMAT = "Basic %s";
    private static final String OAUTH_DEFAULT_SCOPE = "https://graph.windows.net/.default";
    private static final String RESPONSE_TYPE = "token id_token";


    private RestTemplate restTemplate;
    private AzureProperties azureProperties;
    private AzureApiGraphProperties azureApiGraphProperties;

    public TokenResponse getAccessToken(ParamsTokenRequest request) {
        ResponseEntity<TokenResponse> response;
        try {
            response = restTemplate.postForEntity(azureApiGraphProperties.getGetAccessTokenResourcePath(),
                new HttpEntity<>(getBody(request), getHeaders()),
                TokenResponse.class
            );
        } catch (HttpClientErrorException | HttpServerErrorException exception) {
            throw new TenpoException(exception, HttpStatus.UNPROCESSABLE_ENTITY, "ERR-CODE");
        }
        return response.getBody();
    }

    public TokenResponse getLoginAccessToken(ParamsTokenRequest request) {
        ResponseEntity<TokenResponse> response;
        try {
            response = restTemplate.postForEntity(
                    azureApiGraphProperties.getGetLoginAccessTokenResourcePath(),
                    new HttpEntity<>(getLoginBody(request), getHeaders()),
                    TokenResponse.class
            );
        } catch (HttpClientErrorException | HttpServerErrorException exception) {
            throw new TenpoException(exception, HttpStatus.UNPROCESSABLE_ENTITY, "ERR-CODE");
        }
        return response.getBody();
    }

    private MultiValueMap<String, String> getBody(ParamsTokenRequest request) {
        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("scope", OAUTH_DEFAULT_SCOPE);
        map.add("grant_type", request.getGrantType());
        map.add("username", request.getUsername());
        map.add("password", request.getPassword());

        return map;
    }

    private MultiValueMap<String, String> getLoginBody(ParamsTokenRequest request) {
        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("scope", "openid " + azureProperties.getUserClientId() + " offline_access");
        map.add("client_id", azureProperties.getUserClientId());
        map.add("grant_type", request.getGrantType());
        map.add("response_type", RESPONSE_TYPE);
        map.add("username", request.getUsername());
        map.add("password", request.getPassword());

        return map;
    }

    private HttpHeaders getHeaders() {
        String basicAuth = createEncodedText(getClientId(), getClientSecret());

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, String.format(BASIC_TOKEN_FORMAT, basicAuth));
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        return headers;
    }

    private String getClientId() {
        return Optional.ofNullable(azureProperties.getClientId())
                .orElse(System.getenv("AZURE_CLIENT_ID"));
    }

    private String getClientSecret() {
        return Optional.ofNullable(azureProperties.getClientSecret())
                .orElse(System.getenv("AZURE_CLIENT_SECRET"));
    }

    public static String[] decode(final String encodedString) {
        final byte[] decodedBytes = Base64.decodeBase64(encodedString.getBytes());
        final String pair = new String(decodedBytes);
        return pair.split(":", 2);
    }

    public static String createEncodedText(final String username, final String password) {
        final String pair = username + ":" + password;
        final byte[] encodedBytes = Base64.encodeBase64(pair.getBytes());
        return new String(encodedBytes);
    }

}