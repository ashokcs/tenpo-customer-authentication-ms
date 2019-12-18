/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cl.tenpo.customerauthentication.externalservice.azure.dto;

import lombok.*;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 *
 * @author Ramon Rangel Osorio <ramon.rangel@avantica.net>
 */
@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class PasswordProfileRequest {

    @NotNull
    @NotBlank
    private boolean forceChangePasswordNextLogin;

    @NotNull
    @NotBlank
    private String password;
}
