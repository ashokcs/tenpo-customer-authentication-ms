package cl.tenpo.customerauthentication.integration.service;

import cl.tenpo.customerauthentication.CustomerAuthenticationMsApplicationTests;
import cl.tenpo.customerauthentication.api.dto.CreateChallengeRequest;
import cl.tenpo.customerauthentication.dto.CustomerChallengeDTO;
import cl.tenpo.customerauthentication.dto.CustomerTransactionContextDTO;
import cl.tenpo.customerauthentication.model.ChallengeType;
import cl.tenpo.customerauthentication.service.CustomerChallengeService;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;
import java.util.Optional;

@RunWith(SpringRunner.class)
@SpringBootTest
public class CustomerChallengeServiceTests extends CustomerAuthenticationMsApplicationTests {

    @Autowired
    private CustomerChallengeService customerChallengeService;

    @Test
    public void createChallenge() {
        CreateChallengeRequest createChallengeRequest = randomChallengeRequest();
        Optional<CustomerTransactionContextDTO> customerTransactionContextDTO = customerChallengeService.createChallenge(createChallengeRequest);
        Assert.assertTrue("Debe existir", customerTransactionContextDTO.isPresent());
        Assert.assertEquals("External id deben ser iguales",createChallengeRequest.getExternalId(), customerTransactionContextDTO.get().getExternalId());
        Assert.assertEquals("Merchant debe ser igual",createChallengeRequest.getTransactionContext().getTxMerchant(),customerTransactionContextDTO.get().getTxMerchant());
    }

    @Test
    public void createChallengeFindChallenge() {
        CreateChallengeRequest createChallengeRequest = randomChallengeRequest();
        Optional<CustomerTransactionContextDTO> customerTransactionContextDTO = customerChallengeService.createChallenge(createChallengeRequest);
        Assert.assertTrue("Debe existir", customerTransactionContextDTO.isPresent());
        Assert.assertEquals("External id deben ser iguales",createChallengeRequest.getExternalId(), customerTransactionContextDTO.get().getExternalId());
        Assert.assertEquals("Merchant debe ser igual",createChallengeRequest.getTransactionContext().getTxMerchant(),customerTransactionContextDTO.get().getTxMerchant());

        List<CustomerChallengeDTO> customerChallengeDTOList = customerChallengeService.findChallengeByTrxId(customerTransactionContextDTO.get().getId());
        Assert.assertEquals("Debe existir 1 challenge",1,customerChallengeDTOList.size());
    }

    @Test
    public void createSecondChallengeFindChallenge() {
        CreateChallengeRequest createChallengeRequest = randomChallengeRequest();
        Optional<CustomerTransactionContextDTO> customerTransactionContextDTO = customerChallengeService.createChallenge(createChallengeRequest);
        Assert.assertTrue("Debe existir", customerTransactionContextDTO.isPresent());
        Assert.assertEquals("External id deben ser iguales",createChallengeRequest.getExternalId(), customerTransactionContextDTO.get().getExternalId());
        Assert.assertEquals("Merchant debe ser igual",createChallengeRequest.getTransactionContext().getTxMerchant(),customerTransactionContextDTO.get().getTxMerchant());

        List<CustomerChallengeDTO> customerChallengeDTOList = customerChallengeService.findChallengeByTrxId(customerTransactionContextDTO.get().getId());
        Assert.assertEquals("Debe existir 1 challenge",1,customerChallengeDTOList.size());

        createChallengeRequest.setChallengeType(ChallengeType.OTP_MAIL);
        Optional<CustomerTransactionContextDTO> customerTransactionContextDTO2 = customerChallengeService.createChallenge(createChallengeRequest);
        Assert.assertTrue("Debe existir", customerTransactionContextDTO.isPresent());
        Assert.assertEquals("External id deben ser iguales",createChallengeRequest.getExternalId(), customerTransactionContextDTO.get().getExternalId());
        Assert.assertEquals("Merchant debe ser igual",createChallengeRequest.getTransactionContext().getTxMerchant(),customerTransactionContextDTO.get().getTxMerchant());

        List<CustomerChallengeDTO> customerChallengeDTOList2 = customerChallengeService.findChallengeByTrxId(customerTransactionContextDTO.get().getId());
        Assert.assertEquals("Debe existir 1 challenge",2,customerChallengeDTOList2.size());

    }
}
