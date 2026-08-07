package com.platform.application.partner;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;

import com.platform.application.partner.query.SearchPartnersQuery;
import com.platform.domain.partner.Partner;
import com.platform.infrastructure.persistence.partner.PartnerRepository;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@ActiveProfiles("test")
class PartnerForStaffSearchTests {

	@Autowired
	private PartnerRepository partnerRepository;

	@Test
	void accountLoginIdKeywordBuildsAValidJpaQuery() throws Exception {
		PartnerForStaffService service = serviceWithMockedDependencies();
		Method method = PartnerForStaffService.class.getDeclaredMethod(
			"specification",
			SearchPartnersQuery.class
		);
		method.setAccessible(true);
		SearchPartnersQuery query = new SearchPartnersQuery(
			"partner_login",
			List.of(),
			List.of(),
			List.of(),
			List.of(),
			List.of(),
			false,
			null,
			null,
			null,
			null,
			null,
			"desc",
			1,
			15
		);
		@SuppressWarnings("unchecked")
		Specification<Partner> specification = (Specification<Partner>) method.invoke(service, query);

		assertDoesNotThrow(() -> partnerRepository.findAll(specification, PageRequest.of(0, 15)));
	}

	private PartnerForStaffService serviceWithMockedDependencies() throws Exception {
		var constructor = PartnerForStaffService.class.getConstructors()[0];
		Object[] dependencies = Arrays.stream(constructor.getParameterTypes())
			.map(type -> mock(type))
			.toArray();
		return (PartnerForStaffService) constructor.newInstance(dependencies);
	}
}
