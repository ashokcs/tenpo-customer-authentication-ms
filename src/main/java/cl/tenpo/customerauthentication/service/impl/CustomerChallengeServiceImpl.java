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
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.expression.ParseException;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public class CustomerChallengeServiceImpl implements CustomerChallengeService {

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private CustomerTransactionContextRespository customerTransactionContextRespository;

    @Autowired
    private CustomerChallengeRepository customerChallengeRepository;



    @Override
    public void createChallenge(CreateChallengeRequest createChallengeRequest) {
        Optional<CustomerTransactionContextDTO> customerTrx = findByExternalId(createChallengeRequest.getExternalId());

        if(!customerTrx.isPresent()) {
            // Si no existe se crea
            Optional<CustomerTransactionContextDTO> customerTransactionContextDTO =  createTransactionContext(CustomerTransactionContextDTO.builder()
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

            if(customerTransactionContextDTO.isPresent()){
                createCustomerChallenge(
                        CustomerChallengeDTO.builder()
                                .id(UUID.randomUUID())
                                .transactionContextId(customerTrx.get())
                                .created(LocalDateTime.now())
                                .updated(LocalDateTime.now())
                                .challengeType(createChallengeRequest.getChallengeType())
                                .status(ChallengeStatus.OPEN)
                                .build());
            }
        }
        else {
            //Agregar un challenge a una trx existente
            createCustomerChallenge(
                    CustomerChallengeDTO.builder()
                            .id(UUID.randomUUID())
                            .transactionContextId(customerTrx.get())
                            .created(LocalDateTime.now())
                            .updated(LocalDateTime.now())
                            .challengeType(createChallengeRequest.getChallengeType())
                            .status(ChallengeStatus.OPEN)
                            .build()
            );
        }
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
