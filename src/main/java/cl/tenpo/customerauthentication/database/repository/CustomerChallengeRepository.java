package cl.tenpo.customerauthentication.database.repository;

import cl.tenpo.customerauthentication.database.entity.CustomerChallengeEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomerChallengeRepository extends CrudRepository<CustomerChallengeEntity, UUID> {
    List<CustomerChallengeEntity> findByCustomerTransactionId(UUID customerTxId);
    Optional<CustomerChallengeEntity> findById(UUID challengeId);

    @Query("FROM CustomerChallengeEntity cc WHERE cc.customerTransaction.id = :transactionContextId")
    List<CustomerChallengeEntity> findByTransactionContextId(@Param("transactionContextId") UUID transactionContextId);
}
