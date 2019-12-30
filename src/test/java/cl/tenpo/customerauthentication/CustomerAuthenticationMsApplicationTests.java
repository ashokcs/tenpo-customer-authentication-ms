package cl.tenpo.customerauthentication;

import cl.tenpo.customerauthentication.api.dto.CreateChallengeRequest;
import cl.tenpo.customerauthentication.api.dto.TransactionContext;
import cl.tenpo.customerauthentication.model.AmountAndCurrency;
import cl.tenpo.customerauthentication.model.ChallengeType;
import org.apache.commons.lang3.RandomUtils;
import org.springframework.test.context.TestPropertySource;
import java.math.BigDecimal;
import java.util.UUID;

@TestPropertySource(properties = {
		"spring.datasource.driver-class-name = org.h2.Driver",
		"spring.datasource.url = jdbc:h2:mem:test;INIT=CREATE SCHEMA IF NOT EXISTS customer_authentication",
		"spring.jpa.properties.hibernate.dialect = org.hibernate.dialect.H2Dialect",
		"spring.datasource.username = sa",
		"spring.datasource.password = sa",
		"spring.jpa.hibernate.ddl-auto = create-drop",
		"spring.flyway.enabled=false",
		"kafka.listen.auto.start=false",
		"krealo.cloud.users.implement=mock",
		"krealo.cloud.cards.implement=mock"
})
public class CustomerAuthenticationMsApplicationTests {

	protected CreateChallengeRequest randomChallengeRequest(){
		CreateChallengeRequest createChallengeRequest = new CreateChallengeRequest();
		createChallengeRequest.setExternalId(UUID.randomUUID());
		createChallengeRequest.setChallengeType(ChallengeType.OTP_PUSH);
		createChallengeRequest.setCallbackUri("callback.com");
		createChallengeRequest.setTransactionContext(TransactionContext.builder()
				.txAmount(new AmountAndCurrency(new BigDecimal(RandomUtils.nextInt()),152))
				.txCountryCode(152)
				.txPlaceName("Santiago")
				.txMerchant("Tenpito")
				.txType("COMPRA")
				.txOther("REFRIGERADOR LG 3342")
				.build());
		return createChallengeRequest;
	}

}
