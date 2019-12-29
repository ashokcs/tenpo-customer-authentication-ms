package cl.tenpo.customerauthentication.integration.service;

import cl.tenpo.customerauthentication.CustomerAuthenticationMsApplicationTests;
import cl.tenpo.customerauthentication.api.dto.CreateChallengeRequest;
import cl.tenpo.customerauthentication.database.entity.CustomerChallengeEntity;
import cl.tenpo.customerauthentication.database.entity.CustomerTransactionContextEntity;
import cl.tenpo.customerauthentication.database.repository.CustomerChallengeRepository;
import cl.tenpo.customerauthentication.database.repository.CustomerTransactionContextRespository;
import cl.tenpo.customerauthentication.exception.TenpoException;
import cl.tenpo.customerauthentication.model.ChallengeStatus;
import cl.tenpo.customerauthentication.model.ChallengeType;
import cl.tenpo.customerauthentication.model.CustomerTransactionStatus;
import cl.tenpo.customerauthentication.model.NewCustomerChallenge;
import cl.tenpo.customerauthentication.service.CustomerChallengeService;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@RunWith(SpringRunner.class)
@SpringBootTest
public class CustomerChallengeServiceTests extends CustomerAuthenticationMsApplicationTests {

    private final UUID mockUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440000");
    private final String mockTwoFactorCode = "123321";

    @Autowired
    private CustomerChallengeService customerChallengeService;

    @Autowired
    private CustomerChallengeRepository customerChallengeRepository;

    @Autowired
    private CustomerTransactionContextRespository customerTransactionContextRespository;

    @Before
    @After
    public void clearDB() {
        customerChallengeRepository.deleteAll();
        customerTransactionContextRespository.deleteAll();
    }

    @Test
    public void createChallenge_WhenNoTrxNoPreviousChallenge_ThenNewChallengeCreatedAndTrxCreated() {
        UUID userId = UUID.randomUUID();
        CreateChallengeRequest createChallengeRequest = randomChallengeRequest();
        createChallengeRequest.setChallengeType(ChallengeType.OTP_MAIL);

        // Run test
        NewCustomerChallenge newCustomerChallenge = customerChallengeService.createRequestedChallenge(userId, createChallengeRequest);

        Assert.assertNotNull("Debe devolver un nuevo challenge", newCustomerChallenge);
        Assert.assertEquals("Debe tener el codigo que retorna el mock", mockTwoFactorCode, newCustomerChallenge.getCode());
        Assert.assertEquals("Debe tener el mismo tipo enviado", createChallengeRequest.getChallengeType(), newCustomerChallenge.getChallengeType());

        List<CustomerTransactionContextEntity> transactionList = (List<CustomerTransactionContextEntity>) customerTransactionContextRespository.findAll();
        Assert.assertEquals("Debe haber 1 transaccion", 1, transactionList.size());

        CustomerTransactionContextEntity transactionEntity = transactionList.get(0);
        Assert.assertEquals("Debe tener el id del usuario", userId, transactionEntity.getUserId());
        Assert.assertEquals("Debe tener el externalId enviado", createChallengeRequest.getExternalId(), transactionEntity.getExternalId());
        Assert.assertEquals("Debe tener el trxType enviado", createChallengeRequest.getTransactionContext().getTxType(), transactionEntity.getTxType());
        Assert.assertEquals("Debe tener el amount enviado", createChallengeRequest.getTransactionContext().getTxAmount().getValue().stripTrailingZeros(), transactionEntity.getTxAmount().stripTrailingZeros());
        Assert.assertEquals("Debe tener el currency enviado", createChallengeRequest.getTransactionContext().getTxAmount().getCurrencyCode(), transactionEntity.getTxCurrency());
        Assert.assertEquals("Debe tener el merchant enviado", createChallengeRequest.getTransactionContext().getTxMerchant(), transactionEntity.getTxMerchant());
        Assert.assertEquals("Debe tener el other enviado", createChallengeRequest.getTransactionContext().getTxOther(), transactionEntity.getTxOther());
        Assert.assertEquals("Debe tener el placeName enviado", createChallengeRequest.getTransactionContext().getTxPlaceName(), transactionEntity.getTxPlaceName());
        Assert.assertEquals("Debe tener el countryCode enviado", createChallengeRequest.getTransactionContext().getTxCountryCode(), transactionEntity.getTxCountryCode());
        Assert.assertEquals("Debe tener status PENDING", CustomerTransactionStatus.PENDING, transactionEntity.getStatus());

        List<CustomerChallengeEntity> challengeList = customerChallengeRepository.findByTransactionContextId(transactionEntity.getId());
        Assert.assertEquals("La trx debe tener solo 1 challenge asociado", 1, challengeList.size());

        CustomerChallengeEntity challengeEntity = challengeList.get(0);
        Assert.assertEquals("Debe tener el verifier id que retorna el mock", mockUuid, challengeEntity.getVerifierId());
        Assert.assertEquals("Debe tener el id devuelto", challengeEntity.getId(), newCustomerChallenge.getChallengeId());
        Assert.assertEquals("Debe tener status OPEN", ChallengeStatus.OPEN, challengeEntity.getStatus());
    }

    @Test
    public void createChallenge_WhenAlreadyTrxNoPreviousChallenge_ThenNewChallengeCreatedAndNoNewTrx() {
        UUID userId = UUID.randomUUID();
        CreateChallengeRequest createChallengeRequest = randomChallengeRequest();

        // Agrega la transaccion ya existente
        CustomerTransactionContextEntity savedTransactionEntity = createTransactionFromRequest(userId, createChallengeRequest);
        customerTransactionContextRespository.save(savedTransactionEntity);

        // Test
        NewCustomerChallenge newCustomerChallenge = customerChallengeService.createRequestedChallenge(userId, createChallengeRequest);

        Assert.assertNotNull("Debe devolver un nuevo challenge", newCustomerChallenge);
        Assert.assertEquals("Debe tener el codigo que retorna el mock", mockTwoFactorCode, newCustomerChallenge.getCode());
        Assert.assertEquals("Debe tener el mismo tipo enviado", createChallengeRequest.getChallengeType(), newCustomerChallenge.getChallengeType());

        // Debe seguir habiendo solo 1 trx
        List<CustomerTransactionContextEntity> transactionList = (List<CustomerTransactionContextEntity>) customerTransactionContextRespository.findAll();
        Assert.assertEquals("Debe haber solo 1 transaccion", 1, transactionList.size());

        CustomerTransactionContextEntity transactionEntity = transactionList.get(0);
        Assert.assertEquals("Debe tener el id del usuario", userId, transactionEntity.getUserId());
        Assert.assertEquals("Debe tener el externalId enviado", createChallengeRequest.getExternalId(), transactionEntity.getExternalId());
        Assert.assertEquals("Debe tener el trxType enviado", createChallengeRequest.getTransactionContext().getTxType(), transactionEntity.getTxType());
        Assert.assertEquals("Debe tener el amount enviado", createChallengeRequest.getTransactionContext().getTxAmount().getValue().stripTrailingZeros(), transactionEntity.getTxAmount().stripTrailingZeros());
        Assert.assertEquals("Debe tener el currency enviado", createChallengeRequest.getTransactionContext().getTxAmount().getCurrencyCode(), transactionEntity.getTxCurrency());
        Assert.assertEquals("Debe tener el merchant enviado", createChallengeRequest.getTransactionContext().getTxMerchant(), transactionEntity.getTxMerchant());
        Assert.assertEquals("Debe tener el other enviado", createChallengeRequest.getTransactionContext().getTxOther(), transactionEntity.getTxOther());
        Assert.assertEquals("Debe tener el placeName enviado", createChallengeRequest.getTransactionContext().getTxPlaceName(), transactionEntity.getTxPlaceName());
        Assert.assertEquals("Debe tener el countryCode enviado", createChallengeRequest.getTransactionContext().getTxCountryCode(), transactionEntity.getTxCountryCode());
        Assert.assertEquals("Debe tener status PENDING", CustomerTransactionStatus.PENDING, transactionEntity.getStatus());

        List<CustomerChallengeEntity> challengeList = customerChallengeRepository.findByTransactionContextId(transactionEntity.getId());
        Assert.assertEquals("La trx debe tener solo 1 challenge asociado", 1, challengeList.size());

        CustomerChallengeEntity challengeEntity = challengeList.get(0);
        Assert.assertEquals("Debe tener el verifier id que retorna el mock", mockUuid, challengeEntity.getVerifierId());
        Assert.assertEquals("Debe tener el id devuelto", challengeEntity.getId(), newCustomerChallenge.getChallengeId());
        Assert.assertEquals("Debe tener status OPEN", ChallengeStatus.OPEN, challengeEntity.getStatus());

    }

    @Test
    public void createChallenge_whenPreviousChallengeExpired_ThenThrow1200() {
        UUID userId = UUID.randomUUID();
        CreateChallengeRequest createChallengeRequest = randomChallengeRequest();

        // Agrega la transaccion ya existente
        CustomerTransactionContextEntity savedTransactionEntity = createTransactionFromRequest(userId, createChallengeRequest);
        savedTransactionEntity.setCreated(LocalDateTime.now(ZoneId.of("UTC")).minusMinutes(11)); // Creada hace 11 inutos se considera expirada
        customerTransactionContextRespository.save(savedTransactionEntity);

        // Run test
        try {
            customerChallengeService.createRequestedChallenge(userId, createChallengeRequest);
            Assert.fail("No debe pasar por aqui");
        } catch (TenpoException te) {
            Assert.assertEquals("Debe tirar error 1200", "1200", te.getErrorCode());
        } catch (Exception e) {
            Assert.fail("Debe tirar TenpoException");
        }
    }

    @Test
    public void createChallenge_whenPreviousChallengeRecentAndValid_ThenReuse() {
        UUID userId = UUID.randomUUID();
        CreateChallengeRequest createChallengeRequest = randomChallengeRequest();

        // Agrega la transaccion ya existente
        CustomerTransactionContextEntity savedTransactionEntity = createTransactionFromRequest(userId, createChallengeRequest);
        customerTransactionContextRespository.save(savedTransactionEntity);

        // Agrega challenge abierto a esta transaccion
        CustomerChallengeEntity openChallengeEntity = createChallengeEntity(savedTransactionEntity);
        openChallengeEntity.setVerifierId(UUID.fromString("123e4567-e89b-12d3-a456-426655440888")); // distinto uuid
        openChallengeEntity.setChallengeType(createChallengeRequest.getChallengeType());
        openChallengeEntity = customerChallengeRepository.save(openChallengeEntity);

        // Agrega challenge abierto a esta transaccion
        openChallengeEntity = createChallengeEntity(savedTransactionEntity);
        openChallengeEntity.setVerifierId(UUID.fromString("123e4567-e89b-12d3-a456-426655440999")); // distinto uuid
        openChallengeEntity.setChallengeType(createChallengeRequest.getChallengeType());
        openChallengeEntity = customerChallengeRepository.save(openChallengeEntity);

        // Agrega challenge abierto a esta transaccion
        openChallengeEntity = createChallengeEntity(savedTransactionEntity);
        openChallengeEntity.setVerifierId(mockUuid);
        openChallengeEntity.setChallengeType(createChallengeRequest.getChallengeType());
        openChallengeEntity = customerChallengeRepository.save(openChallengeEntity);

        // Run test
        NewCustomerChallenge newCustomerChallenge = customerChallengeService.createRequestedChallenge(userId, createChallengeRequest);
        Assert.assertEquals("Debe ser el mismo challenge", openChallengeEntity.getId(), newCustomerChallenge.getChallengeId());
        Assert.assertEquals("Debe ser el mismo challenge", openChallengeEntity.getChallengeType(), newCustomerChallenge.getChallengeType());
        Assert.assertEquals("Debe tener el codigo devuelto por el mock", mockTwoFactorCode, newCustomerChallenge.getCode());

        List<CustomerChallengeEntity> challengeList = customerChallengeRepository.findByTransactionContextId(savedTransactionEntity.getId());
        Assert.assertEquals("No debe crearse un nuevo challenge asociado, siguen habiendo 3", 3, challengeList.size());
    }

    @Test
    public void createChallenge_whenPreviousChallengeRecentButExpired_ThenCreateNew() {
        UUID userId = UUID.randomUUID();
        CreateChallengeRequest createChallengeRequest = randomChallengeRequest();

        // Agrega la transaccion ya existente
        CustomerTransactionContextEntity savedTransactionEntity = createTransactionFromRequest(userId, createChallengeRequest);
        customerTransactionContextRespository.save(savedTransactionEntity);

        // Agrega challenge abierto a esta transaccion con verifier uuid diferent
        CustomerChallengeEntity openChallengeEntity = createChallengeEntity(savedTransactionEntity);
        openChallengeEntity.setVerifierId(UUID.fromString("123e4567-e89b-12d3-a456-426655440999")); // UUid diferente del mock
        openChallengeEntity.setChallengeType(createChallengeRequest.getChallengeType());
        openChallengeEntity = customerChallengeRepository.save(openChallengeEntity);

        // Run test
        NewCustomerChallenge newCustomerChallenge = customerChallengeService.createRequestedChallenge(userId, createChallengeRequest);
        Assert.assertNotEquals("Debe ser distinto challenge", openChallengeEntity.getId(), newCustomerChallenge.getChallengeId());
        Assert.assertNotEquals("Debe tener el id retornado por el mock", mockUuid, newCustomerChallenge.getChallengeId());
        Assert.assertEquals("Debe ser el mismo tipo de challenge", openChallengeEntity.getChallengeType(), newCustomerChallenge.getChallengeType());
        Assert.assertEquals("Debe tener el codigo devuelto por el mock", mockTwoFactorCode, newCustomerChallenge.getCode());

        List<CustomerChallengeEntity> challengeList = customerChallengeRepository.findByTransactionContextId(savedTransactionEntity.getId());
        Assert.assertEquals("Debe crearse un nuevo challenge", 2, challengeList.size());

        CustomerChallengeEntity storedChalengeEntity = challengeList.stream().filter(c -> c.getVerifierId().equals(mockUuid)).findAny().orElse(null);
        Assert.assertNotNull("Debe existir con el id retornado por el mock", storedChalengeEntity);
        Assert.assertEquals("Debe ser del mismo tipo", openChallengeEntity.getChallengeType(), storedChalengeEntity.getChallengeType());
        Assert.assertEquals("Debe estar en estado open", ChallengeStatus.OPEN, storedChalengeEntity.getStatus());
    }

    @Test
    public void createChallenge_whenPreviousChallengeTooOldAndValid_ThenCreateNew() {
        UUID userId = UUID.randomUUID();
        CreateChallengeRequest createChallengeRequest = randomChallengeRequest();

        // Agrega la transaccion ya existente
        CustomerTransactionContextEntity savedTransactionEntity = createTransactionFromRequest(userId, createChallengeRequest);
        customerTransactionContextRespository.save(savedTransactionEntity);

        // Agrega challenge abierto a esta transaccion con verifier uuid diferent
        CustomerChallengeEntity openChallengeEntity = createChallengeEntity(savedTransactionEntity);
        openChallengeEntity.setVerifierId(UUID.fromString("123e4567-e89b-12d3-a456-426655440999"));
        openChallengeEntity.setChallengeType(createChallengeRequest.getChallengeType());
        openChallengeEntity.setCreated(LocalDateTime.now(ZoneId.of("UTC")).minusSeconds(31)); // Muy viejo
        openChallengeEntity = customerChallengeRepository.save(openChallengeEntity);

        // Run test
        NewCustomerChallenge newCustomerChallenge = customerChallengeService.createRequestedChallenge(userId, createChallengeRequest);
        Assert.assertNotEquals("Debe ser distinto challenge", openChallengeEntity.getId(), newCustomerChallenge.getChallengeId());
        Assert.assertEquals("Debe ser el mismo tipo de challenge", openChallengeEntity.getChallengeType(), newCustomerChallenge.getChallengeType());
        Assert.assertEquals("Debe tener el codigo devuelto por el mock", mockTwoFactorCode, newCustomerChallenge.getCode());

        List<CustomerChallengeEntity> challengeList = customerChallengeRepository.findByTransactionContextId(savedTransactionEntity.getId());
        Assert.assertEquals("Debe crearse un nuevo challenge", 2, challengeList.size());

        // Validar que el nuevo challenge se haya creado correctamente
        CustomerChallengeEntity storedChalengeEntity = challengeList.stream().filter(c -> c.getVerifierId().equals(mockUuid)).findAny().orElse(null);
        Assert.assertNotNull("Debe existir con el id retornado por el mock", storedChalengeEntity);
        Assert.assertEquals("Debe ser del mismo tipo", openChallengeEntity.getChallengeType(), storedChalengeEntity.getChallengeType());
        Assert.assertEquals("Debe estar en estado open", ChallengeStatus.OPEN, storedChalengeEntity.getStatus());
    }

    private CustomerTransactionContextEntity createTransactionFromRequest(UUID userId, CreateChallengeRequest createChallengeRequest) {
        CustomerTransactionContextEntity savedTransactionEntity = new CustomerTransactionContextEntity();
        savedTransactionEntity.setId(UUID.randomUUID());
        savedTransactionEntity.setUserId(userId);
        savedTransactionEntity.setExternalId(createChallengeRequest.getExternalId());
        savedTransactionEntity.setTxType(createChallengeRequest.getTransactionContext().getTxType());
        savedTransactionEntity.setTxAmount(createChallengeRequest.getTransactionContext().getTxAmount().getValue());
        savedTransactionEntity.setTxCurrency(createChallengeRequest.getTransactionContext().getTxAmount().getCurrencyCode());
        savedTransactionEntity.setTxMerchant(createChallengeRequest.getTransactionContext().getTxMerchant());
        savedTransactionEntity.setTxCountryCode(createChallengeRequest.getTransactionContext().getTxCountryCode());
        savedTransactionEntity.setTxPlaceName(createChallengeRequest.getTransactionContext().getTxPlaceName());
        savedTransactionEntity.setTxOther(createChallengeRequest.getTransactionContext().getTxOther());
        savedTransactionEntity.setStatus(CustomerTransactionStatus.PENDING);
        savedTransactionEntity.setCreated(LocalDateTime.now(ZoneId.of("UTC")));
        savedTransactionEntity.setUpdated(LocalDateTime.now(ZoneId.of("UTC")));
        return savedTransactionEntity;
    }

    private CustomerChallengeEntity createChallengeEntity(CustomerTransactionContextEntity transactionEntity) {
        CustomerChallengeEntity customerChallengeEntity = new CustomerChallengeEntity();
        customerChallengeEntity.setId(UUID.randomUUID());
        customerChallengeEntity.setCustomerTransaction(transactionEntity);
        customerChallengeEntity.setVerifierId(UUID.randomUUID());
        customerChallengeEntity.setChallengeType(ChallengeType.OTP_PUSH);
        customerChallengeEntity.setCallbackUri("uri");
        customerChallengeEntity.setStatus(ChallengeStatus.OPEN);
        customerChallengeEntity.setCreated(LocalDateTime.now(ZoneId.of("UTC")));
        customerChallengeEntity.setUpdated(LocalDateTime.now(ZoneId.of("UTC")));
        return customerChallengeEntity;
    }
}
