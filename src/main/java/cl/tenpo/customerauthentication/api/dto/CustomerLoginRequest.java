package cl.tenpo.customerauthentication.api.dto;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CustomerLoginRequest {

    private String rut;
    private String email;
    private String password;

}
