package cl.tenpo.customerauthentication.service.impl;

import cl.tenpo.customerauthentication.api.dto.CreateChallengeRequest;
import cl.tenpo.customerauthentication.constants.ErrorCode;
import cl.tenpo.customerauthentication.database.entity.CustomerChallengeEntity;
import cl.tenpo.customerauthentication.database.entity.CustomerTransactionContextEntity;
import cl.tenpo.customerauthentication.database.repository.CustomerChallengeRepository;
import cl.tenpo.customerauthentication.database.repository.CustomerTransactionContextRespository;
import cl.tenpo.customerauthentication.dto.CustomerChallengeDTO;
import cl.tenpo.customerauthentication.dto.CustomerTransactionContextDTO;
import cl.tenpo.customerauthentication.exception.TenpoException;
import cl.tenpo.customerauthentication.externalservice.verifier.VerifierRestClient;
import cl.tenpo.customerauthentication.externalservice.verifier.dto.GenerateTwoFactorResponse;
import cl.tenpo.customerauthentication.model.ChallengeStatus;
import cl.tenpo.customerauthentication.model.NewCustomerChallenge;
import cl.tenpo.customerauthentication.model.CustomerTransactionStatus;
import cl.tenpo.customerauthentication.service.CustomerChallengeService;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.expression.ParseException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
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

    @Autowired
    private VerifierRestClient verifierRestClient;

    @Override
    public NewCustomerChallenge createRequestedChallenge(UUID userId, CreateChallengeRequest createChallengeRequest) {
        GenerateTwoFactorResponse twoFactorResponse;
        Optional<CustomerTransactionContextDTO> customerTrx = findByExternalId(createChallengeRequest.getExternalId());

        // Si no existe la transaccion se crea
        if (!customerTrx.isPresent()) {
            log.info("[createRequestedChallenge] TransactionContext no existe. Creandolo.");
            customerTrx =  createTransactionContext(CustomerTransactionContextDTO.builder()
                    .id(UUID.randomUUID())
                    .userId(userId)
                    .externalId(createChallengeRequest.getExternalId())
                    .txType(createChallengeRequest.getTransactionContext().getTxType())
                    .txAmount(createChallengeRequest.getTransactionContext().getTxAmount().getValue())
                    .txCurrency(createChallengeRequest.getTransactionContext().getTxAmount().getCurrencyCode())
                    .txMerchant(createChallengeRequest.getTransactionContext().getTxMerchant())
                    .txOther(createChallengeRequest.getTransactionContext().getTxOther())
                    .txPlaceName(createChallengeRequest.getTransactionContext().getTxPlaceName())
                    .txCountryCode(createChallengeRequest.getTransactionContext().getTxCountryCode())
                    .status(CustomerTransactionStatus.PENDING)
                    .created(LocalDateTime.now(ZoneId.of("UTC")))
                    .updated(LocalDateTime.now(ZoneId.of("UTC")))
                    .build());
        }

        LocalDateTime now = LocalDateTime.now(ZoneId.of("UTC"));

        // Revisar si esta transaccion aun no ha expirado
        if (now.isAfter(customerTrx.get().getCreated().plusMinutes(10))) {
            log.info("[createRequestedChallenge] Trx context ya ha expirado.");
            throw new TenpoException(HttpStatus.UNPROCESSABLE_ENTITY, ErrorCode.TRANSACTION_CONTEXT_EXPIRED);
        }

        List<CustomerChallengeEntity> challengeList = customerChallengeRepository.findByTransactionContextId(customerTrx.get().getId());

        // Revisar si existe alguno creado hace menos de 30 segundos, del mismo tipo y estado OPEN
        Optional<CustomerChallengeEntity> ongoingChallenge = challengeList.stream()
                .filter(challenge -> { return ChallengeStatus.OPEN.equals(challenge.getStatus()) &&
                                              createChallengeRequest.getChallengeType().equals(challenge.getChallengeType()) &&
                                              challenge.getCreated().isAfter(LocalDateTime.now(ZoneId.of("UTC")).minusSeconds(30)); })
                .sorted((c1, c2) -> c2.getCreated().compareTo(c1.getCreated()))
                .findFirst();

        // Existe uno reciente?
        if (ongoingChallenge.isPresent()) {

            log.info("[createRequestedChallenge] Hay un challenge reciente de ese tipo.");

            // Buscar su codigo
            twoFactorResponse = verifierRestClient.generateTwoFactorCode(userId, ongoingChallenge.get().getId(), null);

            // No se ha vencido? Retornarlo.
            if (twoFactorResponse.getId().equals(ongoingChallenge.get().getVerifierId())) {
                log.info("[createRequestedChallenge] Hay un challenge reciente y vigente. Reutilizandolo.");
                return NewCustomerChallenge.builder()
                        .challengeId(ongoingChallenge.get().getId())
                        .code(twoFactorResponse.getGeneratedCode())
                        .challengeType(createChallengeRequest.getChallengeType())
                        .build();
            } else {
                log.info("[createRequestedChallenge] Challenge reciente pero vencido. Creando uno nuevo.");
            }
        } else {
            log.info("[createRequestedChallenge] No hay challenge reciente. Creando uno nuevo.");

            // Obtener un nuevo codigo y su id
            twoFactorResponse = verifierRestClient.generateTwoFactorCode(userId, null, null);
        }

        // Se debe crear el nuevo challenge
        Optional<CustomerChallengeDTO> customerChallengeDTO = createCustomerChallenge(CustomerChallengeDTO.builder()
            .id(UUID.randomUUID())
            .customerTransaction(customerTrx.get())
            .verifierId(twoFactorResponse.getId())
            .created(LocalDateTime.now())
            .updated(LocalDateTime.now())
            .challengeType(createChallengeRequest.getChallengeType())
            .status(ChallengeStatus.OPEN)
            .callbackUri(createChallengeRequest.getCallbackUri())
            .build()
        );
        log.info(String.format("[createRequestedChallenge] Challenge nuevo con id:%s", customerChallengeDTO.get().getId()));

        return NewCustomerChallenge.builder()
                .challengeId(customerChallengeDTO.get().getId())
                .code(twoFactorResponse.getGeneratedCode())
                .challengeType(createChallengeRequest.getChallengeType())
                .build();
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
