package cl.tenpo.customerauthentication.integration.service;

import cl.tenpo.customerauthentication.CustomerAuthenticationMsApplicationTests;
import cl.tenpo.customerauthentication.externalservice.kafka.EventProducerService;
import cl.tenpo.customerauthentication.externalservice.kafka.dto.LockUnlockUserDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;


@RunWith(SpringRunner.class)
@SpringBootTest
public class Test extends CustomerAuthenticationMsApplicationTests {

    @Autowired
    EventProducerService eventProducerService;

    @Ignore
    @org.junit.Test
    public void testSendEvent() throws JsonProcessingException {
        eventProducerService.sendLockEvent(LockUnlockUserDto.builder().email("tenpo.test2@mailinator.com").attemp(3).build());
    }

}
