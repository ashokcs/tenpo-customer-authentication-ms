package cl.tenpo.customerauthentication.service;

import cl.tenpo.customerauthentication.api.dto.*;

import java.util.List;
import java.util.UUID;

public interface Customer2faService {

    CustomerLoginResponse login(CustomerLoginRequest request);
    List<String> listChallenge(UUID userId);
    void createChallenge(UUID userId, CreateChallengeRequest request);
    ValidateChallengeResponse validateChallenge(UUID userId, ValidateChallengeRequest request);
    AbortChallengeResponse abortResponse(UUID userId, AbortChallengeRequest request);
}