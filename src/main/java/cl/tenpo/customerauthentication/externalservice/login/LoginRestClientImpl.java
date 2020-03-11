package cl.tenpo.customerauthentication.externalservice.login;

import cl.tenpo.customerauthentication.constants.ErrorCode;
import cl.tenpo.customerauthentication.exception.TenpoException;
import cl.tenpo.customerauthentication.externalservice.login.dto.ErrorResponse;
import cl.tenpo.customerauthentication.externalservice.login.dto.LoginRequestDTO;
import cl.tenpo.customerauthentication.externalservice.login.dto.LoginResponseDTO;
import cl.tenpo.customerauthentication.properties.CloudProps;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;

@Component
@Slf4j
public class LoginRestClientImpl implements LoginRestClient {

    @Autowired
    private CloudProps config;

    @Autowired
    private RestTemplate restTemplate;

    @Override
    public LoginResponseDTO login(LoginRequestDTO loginRequestDTO)throws TenpoException{
        log.info("[login] IN");
        ResponseEntity<LoginResponseDTO> response;
        try {
            HttpEntity<LoginRequestDTO> entity = new HttpEntity<>(loginRequestDTO, getHeaders());
            response = restTemplate.postForEntity(config.getLogin().getGetLoginPath(),entity, LoginResponseDTO.class);
            log.info(""+ response);
        } catch (HttpClientErrorException e) {
            TenpoException exception = buildTenpoException(e);
            log.info("TenpoException [{}] [{}] [{}]",exception.getCode(),exception.getErrorCode(),exception.getMessage());
            throw exception;
        } catch (HttpServerErrorException e) {
            throw new TenpoException(HttpStatus.UNPROCESSABLE_ENTITY, ErrorCode.CONNECTION_ERROR);
        }
        log.info("[login] OUT");
        return response.getBody();
    }

    private  TenpoException buildTenpoException(HttpClientErrorException e) {
        ErrorResponse result = getException(e.getResponseBodyAsString());
        return new TenpoException(e.getStatusCode(), result.getCode(), result.getMessage());
    }
    public ErrorResponse getException(String errorException) {
        ErrorResponse result;
        ObjectMapper mapper = new ObjectMapper();
        try {
            result = mapper.readValue(errorException, ErrorResponse.class);
            return result;
        } catch (IOException io) {
            log.error("NoSuchAlgorithmException : {}", io.getMessage(), io);
            throw new TenpoException(HttpStatus.UNPROCESSABLE_ENTITY, ErrorCode.INTERNAL_ERROR);
        }
    }
    private HttpHeaders getHeaders() {
        HttpHeaders headers = new HttpHeaders();
        String API_KEY_HEADER = "Ocp-Apim-Subscription-Key";
        headers.set(API_KEY_HEADER, config.getLogin().getGetApiKey());
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
}
