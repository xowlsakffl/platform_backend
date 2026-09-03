package com.platform.application.media;

import com.platform.application.auth.PermissionService;
import com.platform.common.error.ApiException;
import com.platform.common.error.ErrorCode;
import com.platform.common.security.AuthenticatedActor;
import com.platform.common.security.AccessPermissions;
import com.platform.domain.media.MediaOwnerType;
import com.platform.domain.media.Media;
import com.platform.domain.category.CategoryStatus;
import com.platform.domain.specialist.SpecialistAllowStatus;
import com.platform.domain.specialist.SpecialistStatus;
import com.platform.domain.partner.PartnerAllowStatus;
import com.platform.domain.partner.PartnerStatus;
import com.platform.infrastructure.persistence.category.CategoryRepository;
import com.platform.infrastructure.persistence.specialist.SpecialistRepository;
import com.platform.infrastructure.persistence.partner.PartnerRepository;
import com.platform.infrastructure.persistence.partner.PartnerBusinessRegistrationRepository;
import org.springframework.stereotype.Component;

@Component
class MediaOwnerPolicy {

	private final PermissionService permissionService;
	private final PartnerRepository partnerRepository;
	private final PartnerBusinessRegistrationRepository businessRegistrationRepository;
	private final CategoryRepository categoryRepository;
	private final SpecialistRepository specialistRepository;

	MediaOwnerPolicy(
		PermissionService permissionService,
		PartnerRepository partnerRepository,
		PartnerBusinessRegistrationRepository businessRegistrationRepository,
		CategoryRepository categoryRepository,
		SpecialistRepository specialistRepository
	) {
		this.permissionService = permissionService;
		this.partnerRepository = partnerRepository;
		this.businessRegistrationRepository = businessRegistrationRepository;
		this.categoryRepository = categoryRepository;
		this.specialistRepository = specialistRepository;
	}

	void requireReadable(AuthenticatedActor actor, MediaOwnerType ownerType, Long ownerId) {
		permissionService.requireStaffPermission(actor, readPermission(ownerType));
		requireActiveOwner(ownerType, ownerId);
	}

	void requireAppReadable(Media media) {
		boolean readable = switch (media.ownerType()) {
			case CATEGORY -> MediaCollectionPolicy.CATEGORY_ICON.equals(media.collection())
				&& categoryRepository.findById(media.ownerId())
					.filter(category -> category.status() == CategoryStatus.ACTIVE)
					.isPresent();
			case SPECIALIST -> MediaCollectionPolicy.SPECIALIST_PROFILE_IMAGE.equals(media.collection())
				&& specialistRepository.findByIdAndDeletedAtIsNullAndPartner_DeletedAtIsNull(media.ownerId())
					.filter(specialist -> specialist.status() == SpecialistStatus.VISIBLE)
					.filter(specialist -> specialist.allowStatus() == SpecialistAllowStatus.APPROVED)
					.filter(specialist -> specialist.partner().status() == PartnerStatus.ACTIVE)
					.filter(specialist -> specialist.partner().allowStatus() == PartnerAllowStatus.APPROVED)
					.isPresent();
			case PARTNER, PARTNER_BUSINESS_REGISTRATION, NOTICE, NOTICE_TEMP -> false;
		};
		if (!readable) {
			throw new ApiException(ErrorCode.NOT_FOUND, "공개된 미디어를 찾을 수 없습니다.");
		}
	}

	private void requireActiveOwner(MediaOwnerType ownerType, Long ownerId) {
		boolean exists = switch (ownerType) {
			case PARTNER -> partnerRepository.existsByIdAndDeletedAtIsNull(ownerId);
			case PARTNER_BUSINESS_REGISTRATION -> businessRegistrationRepository
				.existsByIdAndPartner_DeletedAtIsNull(ownerId);
			case CATEGORY -> categoryRepository.existsById(ownerId);
			case SPECIALIST -> specialistRepository.existsByIdAndDeletedAtIsNullAndPartner_DeletedAtIsNull(ownerId);
			case NOTICE, NOTICE_TEMP -> false;
		};
		if (!exists) {
			throw new ApiException(ErrorCode.NOT_FOUND, "미디어 연결 대상을 찾을 수 없습니다.");
		}
	}

	private String readPermission(MediaOwnerType ownerType) {
		return switch (ownerType) {
			case PARTNER, PARTNER_BUSINESS_REGISTRATION -> AccessPermissions.PARTNER_SHOW;
			case CATEGORY -> AccessPermissions.CATEGORY_MANAGE;
			case SPECIALIST -> AccessPermissions.SPECIALIST_SHOW;
			case NOTICE, NOTICE_TEMP -> AccessPermissions.NOTICE_SHOW;
		};
	}

}
