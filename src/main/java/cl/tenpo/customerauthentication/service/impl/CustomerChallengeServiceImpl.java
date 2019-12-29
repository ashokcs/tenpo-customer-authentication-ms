package cl.tenpo.customerauthentication.service.impl;

import cl.tenpo.customerauthentication.api.dto.CreateChallengeRequest;
import cl.tenpo.customerauthentication.database.entity.CustomerChallengeEntity;
import cl.tenpo.customerauthentication.database.entity.CustomerTransactionContextEntity;
import cl.tenpo.customerauthentication.database.repository.CustomerChallengeRepository;
import cl.tenpo.customerauthentication.database.repository.CustomerTransactionContextRespository;
import cl.tenpo.customerauthentication.dto.CustomerChallengeDTO;
import cl.tenpo.customerauthentication.dto.CustomerTransactionContextDTO;
import cl.tenpo.customerauthentication.exception.TenpoException;
import cl.tenpo.customerauthentication.model.ChallengeStatus;
import cl.tenpo.customerauthentication.model.CustomerTransactionStatus;
import cl.tenpo.customerauthentication.service.CustomerChallengeService;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.expression.ParseException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CustomerChallengeServiceImpl implements CustomerChallengeService {

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private CustomerTransactionContextRespository customerTransactionContextRespository;

    @Autowired
    private CustomerChallengeRepository customerChallengeRepository;



    @Override
    public Optional<CustomerTransactionContextDTO> createChallenge(CreateChallengeRequest createChallengeRequest) {

        Optional<CustomerTransactionContextDTO> customerTrx = findByExternalId(createChallengeRequest.getExternalId());

        if (customerTrx.isPresent()) {
            //Agregar un challenge a una trx existente
            createCustomerChallenge(
                    CustomerChallengeDTO.builder()
                            .id(UUID.randomUUID())
                            .customerTransaction(customerTrx.get())
                            .created(LocalDateTime.now())
                            .updated(LocalDateTime.now())
                            .challengeType(createChallengeRequest.getChallengeType())
                            .status(ChallengeStatus.OPEN)
                            .build()
            );
        } else {
            // Si no existe se crea
            customerTrx =  createTransactionContext(CustomerTransactionContextDTO.builder()
            .id(UUID.randomUUID())
                    .externalId(createChallengeRequest.getExternalId())
                    .txType(createChallengeRequest.getTransactionContext().getTxType())
                    .txAmount(createChallengeRequest.getTransactionContext().getTxAmount().getValue())
                    .txCurrency(createChallengeRequest.getTransactionContext().getTxAmount().getCurrencyCode())
                    .txMerchant(createChallengeRequest.getTransactionContext().getTxMerchant())
                    .txOther(createChallengeRequest.getTransactionContext().getTxOther())
                    .txPlaceName(createChallengeRequest.getTransactionContext().getTxPlaceName())
                    .txCountryCode(createChallengeRequest.getTransactionContext().getTxCountryCode())
                    .status(CustomerTransactionStatus.PENDING)
                    .created(LocalDateTime.now())
                    .updated(LocalDateTime.now())
            .build());

            customerTrx.ifPresent(customerTransactionContextDTO -> createCustomerChallenge(
                    CustomerChallengeDTO.builder()
                            .id(UUID.randomUUID())
                            .customerTransaction(customerTransactionContextDTO)
                            .created(LocalDateTime.now())
                            .updated(LocalDateTime.now())
                            .challengeType(createChallengeRequest.getChallengeType())
                            .status(ChallengeStatus.OPEN)
                            .build()));
        }
        return customerTrx;
    }

    @Override
    public Optional<CustomerTransactionContextDTO> createTransactionContext(CustomerTransactionContextDTO customerTransactionContextDTO) {

        if(customerTransactionContextDTO == null){
            throw new TenpoException(HttpStatus.UNPROCESSABLE_ENTITY,"1200","Objeto no puede ser null");
        }
        CustomerTransactionContextEntity customerTransactionContextEntity = customerTransactionContextRespository.save(convertToEntity(customerTransactionContextDTO));

        return Optional.ofNullable(convertToDto(customerTransactionContextEntity));
    }

    @Override
    public Optional<CustomerChallengeDTO> createCustomerChallenge(CustomerChallengeDTO customerChallengeDTO) {

        if(customerChallengeDTO == null){
            throw new TenpoException(HttpStatus.UNPROCESSABLE_ENTITY,"1200","Objeto no puede ser null");
        }
        CustomerChallengeEntity customerChallengeEntity = customerChallengeRepository.save(convertToEntity(customerChallengeDTO));

        return Optional.ofNullable(convertToDto(customerChallengeEntity));
    }

    @Override
    public Optional<CustomerTransactionContextDTO> findByExternalId(UUID externalId) {
        if(externalId == null){
            throw new TenpoException(HttpStatus.UNPROCESSABLE_ENTITY,"1200","ExternalId  no puede ser null");
        }
        Optional<CustomerTransactionContextEntity> customerTransactionContextEntity = customerTransactionContextRespository.findByExternalId(externalId);
        return customerTransactionContextEntity.map(this::convertToDto);
    }

    @Override
    public List<CustomerChallengeDTO> findChallengeByTrxId(UUID customerTrxId) {
        if(customerTrxId == null){
            throw new TenpoException(HttpStatus.UNPROCESSABLE_ENTITY,"1200","customerTrxId  no puede ser null");
        }
        List<CustomerChallengeEntity> customerChallengeEntityList = customerChallengeRepository.findByCustomerTransactionId(customerTrxId);
        return customerChallengeEntityList.stream().map(e -> convertToDto(e)).collect(Collectors.toList());
    }

    @Override
    public Optional<CustomerTransactionContextDTO> updateTransactionContextStatus(UUID externalId, CustomerTransactionStatus status) {
        log.info("[updateTransactionContextStatus] IN [{}] [{}]", externalId, status);
        Optional<CustomerTransactionContextEntity> customerTransactionContextEntity = customerTransactionContextRespository.findByExternalId(externalId);
        if(customerTransactionContextEntity.isPresent()) {
            customerTransactionContextEntity.get().setStatus(status);
            CustomerTransactionContextEntity customerTx = customerTransactionContextRespository.save(customerTransactionContextEntity.get());
            log.info("[updateTransactionContextStatus] OUT OK");
            return Optional.of(convertToDto(customerTx));
        }
        else {
            log.info("[updateTransactionContextStatus] OUT NOT FOUND");
            return Optional.empty();
        }

    }

    private CustomerTransactionContextEntity convertToEntity(CustomerTransactionContextDTO value) throws ParseException {
        CustomerTransactionContextEntity post = modelMapper.map(value, CustomerTransactionContextEntity.class);
        return post;
    }

    private CustomerTransactionContextDTO convertToDto(CustomerTransactionContextEntity value) throws ParseException {
        CustomerTransactionContextDTO post = modelMapper.map(value, CustomerTransactionContextDTO.class);
        return post;
    }

    private CustomerChallengeEntity convertToEntity(CustomerChallengeDTO value) throws ParseException {
        CustomerChallengeEntity post = modelMapper.map(value, CustomerChallengeEntity.class);
        return post;
    }

    private CustomerChallengeDTO convertToDto(CustomerChallengeEntity value) throws ParseException {
        CustomerChallengeDTO post = modelMapper.map(value, CustomerChallengeDTO.class);
        return post;
    }

}
