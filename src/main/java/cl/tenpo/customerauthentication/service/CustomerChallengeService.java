package cl.tenpo.customerauthentication.service;

import cl.tenpo.customerauthentication.api.dto.CreateChallengeRequest;
import cl.tenpo.customerauthentication.dto.CustomerChallengeDTO;
import cl.tenpo.customerauthentication.dto.CustomerTransactionContextDTO;

import java.util.Optional;
import java.util.UUID;

public interface CustomerChallengeService {

    void createChallenge(CreateChallengeRequest createChallengeRequest);
    Optional<CustomerTransactionContextDTO> createTransactionContext(CustomerTransactionContextDTO CustomerTransactionContextDTO);
    Optional<CustomerChallengeDTO> createCustomerChallenge(CustomerChallengeDTO customerChallengeDTO);
    Optional<CustomerTransactionContextDTO> findByExternalId(UUID externalId);

}
