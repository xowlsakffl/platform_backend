package com.platform.application.partner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.platform.application.auth.OwnershipPolicy;
import com.platform.application.auth.PermissionService;
import com.platform.application.category.CategoryAssignmentService;
import com.platform.application.partner.command.ReplacePartnerOptionsCommand;
import com.platform.application.partner.command.SavePartnerOptionCommand;
import com.platform.common.error.ApiException;
import com.platform.common.security.AuthenticatedActor;
import com.platform.domain.category.Category;
import com.platform.domain.category.CategoryAssignmentTarget;
import com.platform.domain.partner.Partner;
import com.platform.domain.partner.PartnerAllowStatus;
import com.platform.domain.partner.PartnerStatus;
import com.platform.infrastructure.persistence.partner.PartnerOptionRepository;
import com.platform.infrastructure.persistence.partner.PartnerRepository;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@ActiveProfiles("test")
@Import({PartnerOptionForPartnerService.class, PartnerOptionTransactionTests.MockDependencies.class})
class PartnerOptionTransactionTests {

	@Autowired
	private PartnerOptionForPartnerService service;

	@Autowired
	private PartnerRepository partnerRepository;

	@Autowired
	private PartnerOptionRepository optionRepository;

	@Autowired
	private CategoryAssignmentService categoryAssignmentService;

	@Test
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	void rollsBackTheWholeReplacementWhenSpecialistValidationFailsAfterInsert() {
		Partner partner = partnerRepository.saveAndFlush(new Partner(
			"Transaction test partner",
			null,
			null,
			null,
			null,
			null,
			null,
			null,
			PartnerAllowStatus.APPROVED,
			PartnerStatus.ACTIVE
		));
		Category category = mock(Category.class);
		when(category.id()).thenReturn(101L);
		when(category.parentId()).thenReturn(10L);
		when(categoryAssignmentService.requireSelectable(CategoryAssignmentTarget.PARTNER_OPTION, 101L))
			.thenReturn(category);
		when(categoryAssignmentService.isAssigned(CategoryAssignmentTarget.PARTNER, partner.id(), 10L))
			.thenReturn(true);

		SavePartnerOptionCommand invalidSpecialistOption = new SavePartnerOptionCommand(
			101L,
			"Option that must roll back",
			null,
			BigDecimal.valueOf(50_000),
			null,
			60,
			true,
			0,
			List.of(new SavePartnerOptionCommand.SpecialistPriceCommand(999L, null, null))
		);

		assertThrows(ApiException.class, () -> service.replaceForStaff(
			mock(AuthenticatedActor.class),
			partner.id(),
			new ReplacePartnerOptionsCommand(List.of(
				new ReplacePartnerOptionsCommand.Item(null, invalidSpecialistOption)
			))
		));

		assertEquals(0, optionRepository.countByPartner_IdAndDeletedAtIsNull(partner.id()));
	}

	static class MockDependencies {

		@Bean
		OwnershipPolicy ownershipPolicy() {
			return mock(OwnershipPolicy.class);
		}

		@Bean
		PermissionService permissionService() {
			return mock(PermissionService.class);
		}

		@Bean
		CategoryAssignmentService categoryAssignmentService() {
			return mock(CategoryAssignmentService.class);
		}
	}
}
