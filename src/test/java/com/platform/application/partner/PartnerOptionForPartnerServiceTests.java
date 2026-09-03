package com.platform.application.partner;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
import com.platform.domain.partner.PartnerOption;
import com.platform.domain.partner.PartnerStatus;
import com.platform.infrastructure.persistence.partner.PartnerOptionRepository;
import com.platform.infrastructure.persistence.partner.PartnerRepository;
import com.platform.infrastructure.persistence.specialist.SpecialistOptionRepository;
import com.platform.infrastructure.persistence.specialist.SpecialistRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class PartnerOptionForPartnerServiceTests {

	private CategoryAssignmentService categoryAssignmentService;
	private PartnerRepository partnerRepository;
	private PartnerOptionRepository optionRepository;
	private PartnerOptionForPartnerService service;
	private AuthenticatedActor actor;

	@BeforeEach
	void setUp() {
		categoryAssignmentService = mock(CategoryAssignmentService.class);
		partnerRepository = mock(PartnerRepository.class);
		optionRepository = mock(PartnerOptionRepository.class);
		actor = mock(AuthenticatedActor.class);
		service = new PartnerOptionForPartnerService(
			mock(OwnershipPolicy.class),
			mock(PartnerHistoryService.class),
			mock(PermissionService.class),
			categoryAssignmentService,
			partnerRepository,
			optionRepository,
			mock(SpecialistRepository.class),
			mock(SpecialistOptionRepository.class)
		);

		Partner partner = new Partner(
			"Test partner",
			null,
			null,
			null,
			null,
			null,
			null,
			null,
			PartnerAllowStatus.APPROVED,
			PartnerStatus.ACTIVE
		);
		ReflectionTestUtils.setField(partner, "id", 1L);
		when(partnerRepository.findForUpdateByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(partner));
		when(optionRepository.findByPartner_IdAndDeletedAtIsNullOrderBySortOrderAscIdAsc(1L))
			.thenReturn(List.of());
	}

	@Test
	void validatesEveryReplacementBeforeWritingAnyOption() {
		Category validCategory = mock(Category.class);
		when(validCategory.id()).thenReturn(101L);
		when(validCategory.parentId()).thenReturn(10L);
		when(categoryAssignmentService.requireSelectable(CategoryAssignmentTarget.PARTNER_OPTION, 101L))
			.thenReturn(validCategory);
		when(categoryAssignmentService.isAssigned(CategoryAssignmentTarget.PARTNER, 1L, 10L))
			.thenReturn(true);
		when(categoryAssignmentService.requireSelectable(CategoryAssignmentTarget.PARTNER_OPTION, 999L))
			.thenThrow(ApiException.class);

		ReplacePartnerOptionsCommand command = new ReplacePartnerOptionsCommand(List.of(
			new ReplacePartnerOptionsCommand.Item(null, option(101L, "Valid option")),
			new ReplacePartnerOptionsCommand.Item(null, option(999L, "Invalid option"))
		));

		assertThrows(ApiException.class, () -> service.replaceForStaff(actor, 1L, command));
		verify(optionRepository, never()).saveAndFlush(any(PartnerOption.class));
	}

	private SavePartnerOptionCommand option(Long categoryId, String name) {
		return new SavePartnerOptionCommand(
			categoryId,
			name,
			null,
			BigDecimal.valueOf(50_000),
			null,
			60,
			true,
			0,
			List.of()
		);
	}
}
