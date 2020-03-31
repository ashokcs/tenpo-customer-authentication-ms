package cl.tenpo.customerauthentication.externalservice.kafka;

import cl.tenpo.customerauthentication.externalservice.kafka.dto.LockUnlockUserDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EventProducerService {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    public static final String USER_LOGIN_LOCK_PASSWORD = "USER_LOGIN_LOCK_PASSWORD";

    public void sendLockEvent(LockUnlockUserDto lockUnlockUserDto) throws JsonProcessingException {
        log.info(String.format("[sendLockEvent] -> Producing message -> %s", lockUnlockUserDto));
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);
        this.kafkaTemplate.send(USER_LOGIN_LOCK_PASSWORD, objectMapper.writeValueAsString(lockUnlockUserDto));
        log.info("[sendLockEvent] -> Sent");
    }
}
