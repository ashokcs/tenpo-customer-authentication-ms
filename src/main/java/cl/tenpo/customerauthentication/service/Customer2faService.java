package cl.tenpo.customerauthentication.service;

import cl.tenpo.customerauthentication.api.dto.*;
import cl.tenpo.customerauthentication.dto.CustomerTransactionContextDTO;
import cl.tenpo.customerauthentication.externalservice.login.dto.LoginResponseDTO;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.io.IOException;
import java.text.ParseException;
import java.util.List;
import java.util.UUID;

public interface Customer2faService {

    LoginResponseDTO login(CustomerLoginRequest request) throws IOException, ParseException;
    List<String> listChallenge(UUID userId);
    void createChallenge(UUID userId, CreateChallengeRequest request) throws JsonProcessingException;
    ValidateChallengeResponse validateChallenge(UUID userId, ValidateChallengeRequest request) throws JsonProcessingException;
    AbortChallengeResponse abortChallenge(UUID userId, AbortChallengeRequest request) throws JsonProcessingException;
    void validateTransactionContextStatus(CustomerTransactionContextDTO transactionContextDTO, boolean validateCanceled);
    void validateTransactionAttempts(String email, CustomerTransactionContextDTO transactionContextDTO) throws JsonProcessingException;
}