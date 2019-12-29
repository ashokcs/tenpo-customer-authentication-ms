package cl.tenpo.customerauthentication.service.impl;

import cl.tenpo.customerauthentication.api.dto.*;
import cl.tenpo.customerauthentication.component.AzureClient;
import cl.tenpo.customerauthentication.dto.CustomerTransactionContextDTO;
import cl.tenpo.customerauthentication.dto.JwtDTO;
import cl.tenpo.customerauthentication.exception.TenpoException;
import cl.tenpo.customerauthentication.externalservice.azure.dto.TokenResponse;
import cl.tenpo.customerauthentication.externalservice.cards.CardRestClient;
import cl.tenpo.customerauthentication.externalservice.user.UserRestClient;
import cl.tenpo.customerauthentication.externalservice.user.dto.UserResponse;
import cl.tenpo.customerauthentication.externalservice.user.dto.UserStateType;
import cl.tenpo.customerauthentication.model.ChallengeResult;
import cl.tenpo.customerauthentication.model.ChallengeType;
import cl.tenpo.customerauthentication.service.Customer2faService;
import cl.tenpo.customerauthentication.service.CustomerChallengeService;
import cl.tenpo.customerauthentication.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Slf4j
public class Customer2faServiceImpl implements Customer2faService {

    @Autowired
    private AzureClient azureClient;

    @Autowired
    private UserRestClient userRestClient;

    @Autowired
    private CardRestClient cardRestClient;
    @Autowired
    private CustomerChallengeService customerChallengeService;


    @Override
    public TokenResponse login(CustomerLoginRequest request) {
        TokenResponse tokenResponse;
        try {
            //Try Login in AZ AD
            log.info("[login] Try to login into azure ad");
            tokenResponse = azureClient.loginUser(request.getEmail(),request.getPassword());
            log.info("[login] Login succes");
            JwtDTO jwtDTO = JwtUtil.parseJWT(tokenResponse.getAccessToken());
            log.info("[login] Token Parsed");
            Optional<UserResponse> userResponseDto = userRestClient.getUserByProvider(jwtDTO.getOid());
            log.info("[login] Find User by provider");
            // Verificacion de usuario
            if(userResponseDto.isPresent()) {
               if(!userResponseDto.get().getState().equals(UserStateType.ACTIVE)) {
                   log.error("[login] Cliente no activo");
                   throw new TenpoException(HttpStatus.NOT_FOUND,"1150","El cliente no existe o está bloqueado");
               }
               // Si la verificac
                log.error("[login] Tarjeta verifica tarjeta");
               cardRestClient.checkIfCardBelongsToUser(userResponseDto.get().getId(),request.getPan());
            }else {
                log.error("[login] Usuario no existe");
                throw new TenpoException(HttpStatus.NOT_FOUND,"1150","El cliente no existe o está bloqueado");
            }
        } catch (Exception e){
            log.error("[login] Error login on Azure AD");
            throw new TenpoException(HttpStatus.NOT_FOUND,"1150","El cliente no existe o está bloqueado");
        }
        log.info("[login] User+Pan OK");
        return tokenResponse;
    }

    @Override
    public void createChallenge(UUID userId, CreateChallengeRequest request) {

    }

    @Override
    public ValidateChallengeResponse validateChallenge(UUID userId, ValidateChallengeRequest request) {

        Optional<CustomerTransactionContextDTO> customerTransactionContextDTO = customerChallengeService.findByExternalId(request.getExternalId());
        if(!customerTransactionContextDTO.isPresent()){
            log.error("[validateChallenge] No se encontró el external_id. Llame primero a POST");
            throw new TenpoException(HttpStatus.UNPROCESSABLE_ENTITY,"1100", "No se encontró el external_id. Llame primero a POST");
        }
        Optional<UserResponse> userResponseDto;
        try {
            userResponseDto = userRestClient.getUser(userId);
        }catch (Exception e){
            log.error("[validateChallenge] Error al verificar usuario");
            throw new TenpoException(HttpStatus.UNPROCESSABLE_ENTITY,"500", "Error interno del servicio al verificar usuario");
        }

        if(!userResponseDto.isPresent()|| !userResponseDto.get().getState().equals(UserStateType.ACTIVE)){
            log.error("[validateChallenge] El cliente no existe o esta bloqueado");
            throw new TenpoException(HttpStatus.NOT_FOUND,"1150", "El cliente no existe o está bloqueado");
        }
        //TODO: Llamada a verifier-ms con codigo a validar

        ValidateChallengeResponse response = ValidateChallengeResponse.builder()
                                                                    .result(ChallengeResult.AUTH_EXITOSA)
                                                                    .externalId(customerTransactionContextDTO.get().getExternalId()).build();
        return response;
    }

    @Override
    public AbortChallengeResponse abortResponse(UUID userId, AbortChallengeRequest request) {
        return null;
    }

    @Override
    public List<String> listChallenge(UUID userId) {
        Optional<UserResponse> userResponseDto;
        try {
            userResponseDto = userRestClient.getUser(userId);
         }catch (Exception e){
            log.error("[listChallenge] Error al verificar usuario");
            throw new TenpoException(HttpStatus.UNPROCESSABLE_ENTITY,"500","Error desconocido a verificar ");
         }
        if(!userResponseDto.isPresent()|| !userResponseDto.get().getState().equals(UserStateType.ACTIVE)){
            throw new TenpoException(HttpStatus.NOT_FOUND,"1150","El cliente no existe o está bloqueado");
        }
        return Stream.of(ChallengeType.values()).map(Enum::name).collect(Collectors.toList());
    }

}
