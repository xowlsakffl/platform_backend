package com.platform.application.partner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.platform.application.auth.PermissionService;
import com.platform.application.media.MediaCollectionPolicy;
import com.platform.application.media.storage.MediaFileSource;
import com.platform.application.partner.result.BusinessRegistrationOcrResult;
import com.platform.common.security.AuthenticatedActor;
import com.platform.domain.account.AccountActorType;
import com.platform.infrastructure.persistence.partner.PartnerBusinessRegistrationRepository;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class BusinessRegistrationOcrServiceTests {

	@Test
	void marksAnExistingBusinessNumberBeforeRegistration() {
		BusinessRegistrationOcrClient ocrClient = mock(BusinessRegistrationOcrClient.class);
		PartnerBusinessRegistrationRepository repository = mock(PartnerBusinessRegistrationRepository.class);
		MediaFileSource file = mock(MediaFileSource.class);
		BusinessRegistrationOcrResult ocrResult = new BusinessRegistrationOcrResult(
			"123-45-67890",
			"플랫폼뷰티",
			"홍길동",
			"서울특별시 강남구 테헤란로 1",
			"2025년 12월 27일",
			Map.of(),
			false,
			false
		);
		when(file.size()).thenReturn(1024L);
		when(file.contentType()).thenReturn("image/png");
		when(ocrClient.analyze(file)).thenReturn(ocrResult);
		when(repository.existsByBusinessNumber("1234567890")).thenReturn(true);

		BusinessRegistrationOcrService service = new BusinessRegistrationOcrService(
			new PermissionService(),
			new MediaCollectionPolicy(),
			ocrClient,
			new PartnerBusinessNumberPolicy(),
			repository
		);

		BusinessRegistrationOcrResult result = service.analyze(partnerActor(), file);

		assertThat(result.alreadyRegistered()).isTrue();
	}

	@Test
	void checksEditedBusinessNumberAvailability() {
		BusinessRegistrationOcrClient ocrClient = mock(BusinessRegistrationOcrClient.class);
		PartnerBusinessRegistrationRepository repository = mock(PartnerBusinessRegistrationRepository.class);
		when(repository.existsByBusinessNumber("1234567890")).thenReturn(true);

		BusinessRegistrationOcrService service = new BusinessRegistrationOcrService(
			new PermissionService(),
			new MediaCollectionPolicy(),
			ocrClient,
			new PartnerBusinessNumberPolicy(),
			repository
		);

		assertThat(service.availability(partnerActor(), "123-45-67890").alreadyRegistered()).isTrue();
	}

	private AuthenticatedActor partnerActor() {
		return new AuthenticatedActor(
			AccountActorType.PARTNER,
			1L,
			null,
			"session",
			"partner@platform.local",
			"partner",
			null,
			null,
			Set.of()
		);
	}
}
