package cl.tenpo.customerauthentication.externalservice.user;


import cl.tenpo.customerauthentication.externalservice.user.dto.UserResponse;
import cl.tenpo.customerauthentication.externalservice.user.dto.UserStateType;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpServerErrorException;

import java.util.Optional;
import java.util.UUID;

@Component
@Slf4j
@ConditionalOnProperty(name = "krealo.cloud.users.implement", havingValue = "mock")
public class UserRestClientMock implements UserRestClient {

    private static int retryCount= 0;
    public static final UUID USER_NOT_FOUND = UUID.fromString("7089a888-80cd-4e3c-bfd7-202e51ceb523");
    public static final UUID USER_BLOQUED = UUID.fromString("ee66fafd-607a-4e01-be70-57edb0bcf478");
    public static final UUID USER_FAIL_RETRY = UUID.fromString("f7e58ac8-fee6-46ff-a86a-e59b3ec9ecd4");

    @Override
    public Optional<UserResponse> getUser(UUID userId) {

        log.info("[getUser] IN");
        if( USER_FAIL_RETRY.equals(userId) && RandomUtils.nextInt(1,10) == 2 ) {
            log.info("[getUser] Return error retyr: {}",retryCount);
            retryCount++;
            throw new HttpServerErrorException(HttpStatus.SERVICE_UNAVAILABLE);
        }
        log.info("[getUser] Retry: {}",retryCount);
        UserResponse userResponse = new UserResponse();
        userResponse.setId(userId);
        userResponse.setTributaryIdentifier("1-9");


        if(USER_NOT_FOUND.equals(userId)){
            log.info("[getUser] Empty");
            return Optional.empty();
        }

        else if(USER_BLOQUED.equals(userId)){
            log.info("[getUser] Bloqued");
            userResponse.setState(UserStateType.BLOCKED);
            return Optional.of(userResponse);
        }
        else {
            log.info("[getUser] Out Active");
            userResponse.setState(UserStateType.ACTIVE);
            return Optional.of(userResponse);
        }

    }

}
