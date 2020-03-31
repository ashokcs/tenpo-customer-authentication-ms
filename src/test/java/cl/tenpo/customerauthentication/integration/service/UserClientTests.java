package cl.tenpo.customerauthentication.integration.service;

import cl.tenpo.customerauthentication.CustomerAuthenticationMsApplicationTests;
import cl.tenpo.customerauthentication.externalservice.user.UserRestClient;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.UUID;

@RunWith(SpringRunner.class)
@SpringBootTest
public class UserClientTests extends CustomerAuthenticationMsApplicationTests {

    @Autowired
    private UserRestClient userRestClient;

    @Test
    public void testRetry() throws IOException {
        userRestClient.getUser(UUID.randomUUID());
    }
}
