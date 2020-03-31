package cl.tenpo.customerauthentication.util;

import cl.tenpo.customerauthentication.dto.JwtDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.postgresql.util.Base64;

import java.io.IOException;
import java.text.ParseException;

public class JwtUtil {

    public static JwtDTO parseJWT(String token) throws ParseException, IOException {
        String[] splited = token.split("\\.");
        String base64EncodedBody = splited[1];
        String body = new String(Base64.decode(base64EncodedBody));
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(body.concat("}"),JwtDTO.class);
    }

}
