package cl.tenpo.customerauthentication.database.repository;

import cl.tenpo.customerauthentication.database.entity.CustomerTransactionContextEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomerTransactionContextRespository extends CrudRepository<CustomerTransactionContextEntity, UUID> {
    Optional<CustomerTransactionContextEntity> findByExternalId(String externalId);
}
