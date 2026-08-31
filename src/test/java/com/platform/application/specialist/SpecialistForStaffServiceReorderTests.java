package com.platform.application.specialist;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.platform.application.auth.PermissionService;
import com.platform.application.specialist.command.ReorderSpecialistsForStaffCommand;
import com.platform.common.error.ApiException;
import com.platform.common.security.AuthenticatedActor;
import com.platform.domain.partner.Partner;
import com.platform.domain.specialist.Specialist;
import com.platform.infrastructure.persistence.account.AccountStaffRepository;
import com.platform.infrastructure.persistence.partner.PartnerRepository;
import com.platform.infrastructure.persistence.specialist.SpecialistRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class SpecialistForStaffServiceReorderTests {

	private final PermissionService permissionService = mock(PermissionService.class);
	private final SpecialistRepository specialistRepository = mock(SpecialistRepository.class);
	private final PartnerRepository partnerRepository = mock(PartnerRepository.class);
	private final AuthenticatedActor actor = mock(AuthenticatedActor.class);
	private final SpecialistForStaffService service = new SpecialistForStaffService(
		permissionService,
		specialistRepository,
		partnerRepository,
		mock(AccountStaffRepository.class),
		mock(SpecialistWriteService.class),
		mock(SpecialistResultAssembler.class),
		mock(SpecialistHistoryService.class),
		mock(SpecialistLifecycleService.class)
	);

	@Test
	void reordersEveryActiveSpecialistInThePartner() {
		long partnerId = 10L;
		Specialist first = specialist(1L);
		Specialist second = specialist(2L);
		when(partnerRepository.findForUpdateByIdAndDeletedAtIsNull(partnerId))
			.thenReturn(Optional.of(mock(Partner.class)));
		when(specialistRepository.findForUpdateByPartner_IdAndDeletedAtIsNullOrderBySortOrderAscIdAsc(partnerId))
			.thenReturn(List.of(first, second));

		var result = service.reorder(
			actor,
			partnerId,
			new ReorderSpecialistsForStaffCommand(List.of(2L, 1L))
		);

		assertThat(result.specialistIds()).containsExactly(2L, 1L);
		verify(second).changeSortOrder(0);
		verify(first).changeSortOrder(1);
		verify(specialistRepository).flush();
	}

	@Test
	void rejectsDuplicateOrMissingSpecialistIds() {
		long partnerId = 10L;
		Specialist first = specialist(1L);
		Specialist second = specialist(2L);
		when(partnerRepository.findForUpdateByIdAndDeletedAtIsNull(partnerId))
			.thenReturn(Optional.of(mock(Partner.class)));
		when(specialistRepository.findForUpdateByPartner_IdAndDeletedAtIsNullOrderBySortOrderAscIdAsc(partnerId))
			.thenReturn(List.of(first, second));

		assertThatThrownBy(() -> service.reorder(
			actor,
			partnerId,
			new ReorderSpecialistsForStaffCommand(List.of(1L, 1L))
		))
			.isInstanceOf(ApiException.class)
			.hasMessageContaining("전문가 순서");
	}

	private Specialist specialist(Long id) {
		Specialist specialist = mock(Specialist.class);
		when(specialist.id()).thenReturn(id);
		return specialist;
	}
}
