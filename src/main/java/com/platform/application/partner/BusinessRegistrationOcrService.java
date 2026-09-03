package com.platform.application.partner;

import com.platform.application.auth.PermissionService;
import com.platform.application.media.MediaCollectionPolicy;
import com.platform.application.media.storage.MediaFileSource;
import com.platform.application.partner.result.BusinessRegistrationOcrResult;
import com.platform.application.partner.result.BusinessNumberAvailabilityResult;
import com.platform.common.error.ApiException;
import com.platform.common.error.ErrorCode;
import com.platform.common.security.AuthenticatedActor;
import com.platform.domain.account.AccountActorType;
import com.platform.infrastructure.persistence.partner.PartnerBusinessRegistrationRepository;
import org.springframework.stereotype.Service;

@Service
public class BusinessRegistrationOcrService {

	private final PermissionService permissionService;
	private final MediaCollectionPolicy mediaCollectionPolicy;
	private final BusinessRegistrationOcrClient ocrClient;
	private final PartnerBusinessNumberPolicy businessNumberPolicy;
	private final PartnerBusinessRegistrationRepository businessRegistrationRepository;

	public BusinessRegistrationOcrService(
		PermissionService permissionService,
		MediaCollectionPolicy mediaCollectionPolicy,
		BusinessRegistrationOcrClient ocrClient,
		PartnerBusinessNumberPolicy businessNumberPolicy,
		PartnerBusinessRegistrationRepository businessRegistrationRepository
	) {
		this.permissionService = permissionService;
		this.mediaCollectionPolicy = mediaCollectionPolicy;
		this.ocrClient = ocrClient;
		this.businessNumberPolicy = businessNumberPolicy;
		this.businessRegistrationRepository = businessRegistrationRepository;
	}

	public BusinessRegistrationOcrResult analyze(AuthenticatedActor actor, MediaFileSource file) {
		permissionService.requireActor(actor, AccountActorType.PARTNER);
		if (file == null || file.size() <= 0) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "사업자등록증을 등록해 주세요.");
		}
		mediaCollectionPolicy.validateBusinessRegistrationInput(file.contentType(), file.size());
		BusinessRegistrationOcrResult result = ocrClient.analyze(file);
		return result.withAlreadyRegistered(alreadyRegistered(result.businessNumber()));
	}

	public BusinessNumberAvailabilityResult availability(AuthenticatedActor actor, String businessNumber) {
		permissionService.requireActor(actor, AccountActorType.PARTNER);
		String normalized = businessNumberPolicy.normalize(businessNumber);
		return new BusinessNumberAvailabilityResult(
			businessRegistrationRepository.existsByBusinessNumber(normalized)
		);
	}

	private boolean alreadyRegistered(String businessNumber) {
		if (businessNumber == null || !businessNumber.matches("^(?:[0-9]{10}|[0-9]{3}-[0-9]{2}-[0-9]{5})$")) {
			return false;
		}
		return businessRegistrationRepository.existsByBusinessNumber(
			businessNumberPolicy.normalize(businessNumber)
		);
	}
}
