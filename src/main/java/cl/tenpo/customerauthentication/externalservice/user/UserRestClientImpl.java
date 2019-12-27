package cl.tenpo.customerauthentication.externalservice.user;

import cl.tenpo.customerauthentication.externalservice.user.dto.UserResponse;
import cl.tenpo.customerauthentication.externalservice.user.dto.UserResponseDto;
import cl.tenpo.customerauthentication.properties.CloudProps;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
@Slf4j
@ConditionalOnProperty(name = "krealo.cloud.users.implement", havingValue = "real")
public class UserRestClientImpl implements UserRestClient {

    private static final String PROVIDER = "AZURE_AD";
    private static final String USER_PROVIDER_ID = "userProviderId";

    @Autowired
    private CloudProps config;

    @Autowired
    private RestTemplate restTemplate;

    @Override
    public Optional<UserResponse> getUser(UUID userId) throws IOException {
        try {
            log.info("[getUser] get user by Id: [{}]", userId.toString());
            Map<String, String> map = new HashMap<>();
            map.put("userId", userId.toString());
            log.debug("[getUser] URL [{}]",config.getUsers().getGetUserByIdResourcePath());

            // Recibiendo respuesta como string y luego mapeandola
            String response = restTemplate.getForObject(config.getUsers().getGetUserByIdResourcePath(), String.class, map);
            log.debug("[getUser] Response: [{}]", response);
            ObjectMapper objectMapper = new ObjectMapper();
            UserResponseDto userResponseDto = objectMapper.readValue(response, UserResponseDto.class);
            log.debug("[getUser] userResponseDto: [{}]", userResponseDto);
            return Optional.of(userResponseDto.getUser());
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error("[getUser] User not found ",e);
            throw (e);
        }catch (Exception e){
            log.error("[getUser] Exception:",e);
            throw (e);
        }
    }

    @Override
    public Optional<UserResponse> getUserByProvider(UUID providerId) throws IOException {
        try {

            Map<String, String> map = new HashMap<>();
            map.put("provider", PROVIDER);
            log.info("[getUserByProvider] URL: {}", config.getUsers().getGetUserByProvider());
            log.info("[getUserByProvider] URL Build: {}", String.format("%s?userProviderId=%s",config.getUsers().getGetUserByProvider(),providerId.toString()));
            String response = restTemplate.getForObject(String.format("%s?userProviderId=%s",config.getUsers().getGetUserByProvider(),providerId.toString()), String.class, map);
            log.debug("[getUserByProvider] Response: [{}]", response);

            ObjectMapper objectMapper = new ObjectMapper();
            UserResponseDto userResponseDto = objectMapper.readValue(response, UserResponseDto.class);

            log.debug("[getUserByProvider] userResponseDto: [{}]", userResponseDto);
            return Optional.of(userResponseDto.getUser());
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error("[getUserByProvider] User not found ",e);
            throw (e);
        }catch (Exception e){
            log.error("[getUserByProvider] Exception:",e);
            throw (e);
        }
    }
}
