package cl.tenpo.customerauthentication.service.impl;

import cl.tenpo.customerauthentication.api.dto.*;
import cl.tenpo.customerauthentication.component.AzureClient;
import cl.tenpo.customerauthentication.dto.JwtDTO;
import cl.tenpo.customerauthentication.exception.TenpoException;
import cl.tenpo.customerauthentication.externalservice.azure.dto.TokenResponse;
import cl.tenpo.customerauthentication.externalservice.cards.CardRestClient;
import cl.tenpo.customerauthentication.externalservice.user.UserRestClient;
import cl.tenpo.customerauthentication.externalservice.user.dto.UserResponse;
import cl.tenpo.customerauthentication.externalservice.user.dto.UserStateType;
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
            tokenResponse = azureClient.loginUser(request.getEmail(),request.getPassword());
            JwtDTO jwtDTO = JwtUtil.parseJWT(tokenResponse.getAccessToken());
            Optional<UserResponse> userResponseDto = userRestClient.getUserByProvider(jwtDTO.getOid());
            // Verificacion de usuario
            if(userResponseDto.isPresent()){
               if(!userResponseDto.get().getState().equals(UserStateType.ACTIVE)){
                   throw new TenpoException(HttpStatus.NOT_FOUND,"1150","El cliente no existe o está bloqueado");
               }
               // Si la verificac
               cardRestClient.checkIfCardBelongsToUser(userResponseDto.get().getId(),request.getPan());
            }else {
                throw new TenpoException(HttpStatus.NOT_FOUND,"1150","El cliente no existe o está bloqueado");
            }
        } catch (Exception e){
            throw new TenpoException(HttpStatus.NOT_FOUND,"1150","El cliente no existe o está bloqueado");
        }

        return tokenResponse;
    }

    @Override
    public void createChallenge(UUID userId, CreateChallengeRequest request) {

    }

    @Override
    public ValidateChallengeResponse validateChallenge(UUID userId, ValidateChallengeRequest request) {

        return null;
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
