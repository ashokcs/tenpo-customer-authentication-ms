package cl.tenpo.customerauthentication.service;

import cl.tenpo.customerauthentication.api.dto.CreateChallengeRequest;
import cl.tenpo.customerauthentication.dto.CustomerChallengeDTO;
import cl.tenpo.customerauthentication.dto.CustomerTransactionContextDTO;
import cl.tenpo.customerauthentication.model.CustomerTransactionStatus;
import cl.tenpo.customerauthentication.model.NewCustomerChallenge;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CustomerChallengeService {

    NewCustomerChallenge createRequestedChallenge(UUID userId, CreateChallengeRequest createChallengeRequest) throws JsonProcessingException;
    Optional<CustomerTransactionContextDTO> createTransactionContext(CustomerTransactionContextDTO CustomerTransactionContextDTO);
    Optional<CustomerChallengeDTO> createCustomerChallenge(CustomerChallengeDTO customerChallengeDTO);
    Optional<CustomerTransactionContextDTO> findByExternalId(String externalId);
    List<CustomerChallengeDTO> findChallengeByTrxId(UUID customerTrxId);
    Optional<CustomerTransactionContextDTO> updateTransactionContextStatus(UUID id, CustomerTransactionStatus status);
    Optional<CustomerTransactionContextDTO> addTransactionContextAttempt(UUID id);
}
