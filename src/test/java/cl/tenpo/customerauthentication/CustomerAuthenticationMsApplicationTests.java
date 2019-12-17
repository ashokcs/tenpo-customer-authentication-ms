package cl.tenpo.customerauthentication;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = {
		"spring.datasource.driver-class-name = org.h2.Driver",
		"spring.datasource.url = jdbc:h2:mem:test;INIT=CREATE SCHEMA IF NOT EXISTS customer_authentication",
		"spring.jpa.properties.hibernate.dialect = org.hibernate.dialect.H2Dialect",
		"spring.datasource.username = sa",
		"spring.datasource.password = sa",
		"spring.jpa.hibernate.ddl-auto = create-drop",
		"spring.flyway.enabled=false",
		"kafka.listen.auto.start=false"
})
class CustomerAuthenticationMsApplicationTests {

}
