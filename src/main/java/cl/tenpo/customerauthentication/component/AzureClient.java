package cl.tenpo.customerauthentication.component;

import cl.tenpo.customerauthentication.externalservice.azure.AzureADRestClient;
import cl.tenpo.customerauthentication.externalservice.azure.dto.ParamsTokenRequest;
import cl.tenpo.customerauthentication.externalservice.azure.dto.TokenResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
@Slf4j
public class AzureClient {

    private final AzureADRestClient azureADRestClient;

    public TokenResponse loginUser(String username, String password) {
        return azureADRestClient.getLoginAccessToken(
                ParamsTokenRequest.builder()
                        .grantType("password")
                        .username(username)
                        .password(password)
                        .build()
        );
    }

    private String getAppAccessToken() {
        return azureADRestClient.getAccessToken(ParamsTokenRequest.builder()
                .grantType("client_credentials")
                .build())
                .getAccessToken();
    }
}