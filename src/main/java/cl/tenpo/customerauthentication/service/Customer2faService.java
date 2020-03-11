package cl.tenpo.customerauthentication.service;

import cl.tenpo.customerauthentication.api.dto.*;
import cl.tenpo.customerauthentication.dto.CustomerTransactionContextDTO;
import cl.tenpo.customerauthentication.externalservice.login.dto.LoginResponseDTO;

import java.io.IOException;
import java.text.ParseException;
import java.util.List;
import java.util.UUID;

public interface Customer2faService {

    LoginResponseDTO login(CustomerLoginRequest request) throws IOException, ParseException;
    List<String> listChallenge(UUID userId);
    void createChallenge(UUID userId, CreateChallengeRequest request);
    ValidateChallengeResponse validateChallenge(UUID userId, ValidateChallengeRequest request);
    AbortChallengeResponse abortChallenge(UUID userId, AbortChallengeRequest request);
    void validateTransactionContextStatus(CustomerTransactionContextDTO transactionContextDTO, boolean validateCanceled);
}