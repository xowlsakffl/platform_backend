package com.platform.application.partner;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.platform.application.auth.OwnershipPolicy;
import com.platform.application.auth.PermissionService;
import com.platform.application.category.CategoryAssignmentService;
import com.platform.application.category.result.CategoryReferenceResult;
import com.platform.common.error.ApiException;
import com.platform.domain.category.CategoryAssignmentTarget;
import com.platform.domain.partner.PartnerOption;
import com.platform.infrastructure.persistence.partner.PartnerOptionRepository;
import com.platform.infrastructure.persistence.partner.PartnerRepository;
import com.platform.infrastructure.persistence.specialist.SpecialistOptionRepository;
import com.platform.infrastructure.persistence.specialist.SpecialistRepository;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PartnerOptionCategoryValidationTests {

	private CategoryAssignmentService categoryAssignmentService;
	private PartnerOptionRepository optionRepository;
	private PartnerOptionForPartnerService service;

	@BeforeEach
	void setUp() {
		categoryAssignmentService = mock(CategoryAssignmentService.class);
		optionRepository = mock(PartnerOptionRepository.class);
		service = new PartnerOptionForPartnerService(
			mock(OwnershipPolicy.class),
			mock(PermissionService.class),
			categoryAssignmentService,
			mock(PartnerRepository.class),
			optionRepository,
			mock(SpecialistRepository.class),
			mock(SpecialistOptionRepository.class)
		);
	}

	@Test
	void categoryChangeIsAllowedWhenEveryOptionBelongsToTheSelectedCategory() {
		Long partnerId = 10L;
		Long categoryId = 20L;
		Long optionId = 30L;
		PartnerOption option = option(optionId);
		when(optionRepository.findByPartner_IdAndDeletedAtIsNullOrderBySortOrderAscIdAsc(partnerId))
			.thenReturn(List.of(option));
		when(categoryAssignmentService.referencesByTargetIds(
			eq(CategoryAssignmentTarget.PARTNER_OPTION),
			anyList()
		)).thenReturn(Map.of(optionId, List.of(category(40L, categoryId))));

		assertThatCode(() -> service.validatePartnerCategoryChange(partnerId, categoryId))
			.doesNotThrowAnyException();
	}

	@Test
	void categoryChangeIsRejectedWhenAnOptionBelongsToAnotherCategory() {
		Long partnerId = 10L;
		Long categoryId = 20L;
		Long optionId = 30L;
		PartnerOption option = option(optionId);
		when(optionRepository.findByPartner_IdAndDeletedAtIsNullOrderBySortOrderAscIdAsc(partnerId))
			.thenReturn(List.of(option));
		when(categoryAssignmentService.referencesByTargetIds(
			eq(CategoryAssignmentTarget.PARTNER_OPTION),
			anyList()
		)).thenReturn(Map.of(optionId, List.of(category(40L, 99L))));

		assertThatThrownBy(() -> service.validatePartnerCategoryChange(partnerId, categoryId))
			.isInstanceOf(ApiException.class)
			.hasMessageContaining("options belong to another category");
	}

	private PartnerOption option(Long id) {
		PartnerOption option = mock(PartnerOption.class);
		when(option.id()).thenReturn(id);
		return option;
	}

	private CategoryReferenceResult category(Long id, Long parentId) {
		return new CategoryReferenceResult(id, "옵션 분류", "OPTION", "업체 > 옵션 분류", parentId, 2, true);
	}
}
