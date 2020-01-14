package cl.tenpo.customerauthentication.unit.controller;

import cl.tenpo.customerauthentication.CustomerAuthenticationMsApplicationTests;
import cl.tenpo.customerauthentication.api.controller.CustomerAuthenticactionController;
import cl.tenpo.customerauthentication.api.dto.ValidateChallengeRequest;
import cl.tenpo.customerauthentication.api.dto.ValidateChallengeResponse;
import cl.tenpo.customerauthentication.model.ChallengeResult;
import cl.tenpo.customerauthentication.service.Customer2faService;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.UUID;

import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@SpringBootTest
public class CustomerAuthentictionControllerValidateChallengeTest extends CustomerAuthenticationMsApplicationTests {

    @Autowired
    CustomerAuthenticactionController customerAuthenticactionController;

    @MockBean
    Customer2faService customer2faService;

    @Test
    public void test() {
        UUID userId = UUID.randomUUID();
        ValidateChallengeRequest request = ValidateChallengeRequest.builder()
                .externalId(UUID.randomUUID().toString())
                .response("1234")
                .build();

        ValidateChallengeResponse response = ValidateChallengeResponse.builder()
                .externalId(request.getExternalId())
                .result(ChallengeResult.AUTH_EXITOSA)
                .build();

        when(customer2faService.validateChallenge(userId, request))
                .thenReturn(response);

        ResponseEntity<ValidateChallengeResponse> responseEntity = customerAuthenticactionController.validateChallenge(userId, request);
        Assert.assertEquals("Debe retornar 401", HttpStatus.CREATED, responseEntity.getStatusCode());
        Assert.assertNotNull("Debe tener body", responseEntity.getBody());
        Assert.assertEquals("Debe retornar la respuesta", response.getExternalId(), responseEntity.getBody().getExternalId());
        Assert.assertEquals("Debe retornar la respuesta", response.getResult(), responseEntity.getBody().getResult());
    }
}
