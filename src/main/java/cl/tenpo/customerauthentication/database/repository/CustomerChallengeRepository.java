package cl.tenpo.customerauthentication.database.repository;

import cl.tenpo.customerauthentication.database.entity.CustomerChallengeEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CustomerChallengeRepository extends CrudRepository<CustomerChallengeEntity, UUID> {
    List<CustomerChallengeEntity> findByCustomerTransactionId(UUID customerTxId);
}
