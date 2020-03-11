package cl.tenpo.customerauthentication.externalservice.login;

import cl.tenpo.customerauthentication.externalservice.login.dto.LoginRequestDTO;
import cl.tenpo.customerauthentication.externalservice.login.dto.LoginResponseDTO;

public interface LoginRestClient {
    LoginResponseDTO login(LoginRequestDTO loginRequestDTO);
}
