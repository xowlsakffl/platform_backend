package com.medi.application.media;

import com.medi.application.auth.PermissionService;
import com.medi.common.error.ApiException;
import com.medi.common.error.ErrorCode;
import com.medi.common.security.AuthenticatedActor;
import com.medi.common.security.AccessPermissions;
import com.medi.domain.media.MediaOwnerType;
import com.medi.domain.media.Media;
import com.medi.domain.category.CategoryStatus;
import com.medi.domain.doctor.DoctorAllowStatus;
import com.medi.domain.doctor.DoctorStatus;
import com.medi.domain.hospital.HospitalAllowStatus;
import com.medi.domain.hospital.HospitalStatus;
import com.medi.infrastructure.persistence.category.CategoryRepository;
import com.medi.infrastructure.persistence.doctor.DoctorRepository;
import com.medi.infrastructure.persistence.hospital.HospitalRepository;
import com.medi.infrastructure.persistence.hospital.HospitalBusinessRegistrationRepository;
import org.springframework.stereotype.Component;

@Component
class MediaOwnerPolicy {

	private final PermissionService permissionService;
	private final HospitalRepository hospitalRepository;
	private final HospitalBusinessRegistrationRepository businessRegistrationRepository;
	private final CategoryRepository categoryRepository;
	private final DoctorRepository doctorRepository;

	MediaOwnerPolicy(
		PermissionService permissionService,
		HospitalRepository hospitalRepository,
		HospitalBusinessRegistrationRepository businessRegistrationRepository,
		CategoryRepository categoryRepository,
		DoctorRepository doctorRepository
	) {
		this.permissionService = permissionService;
		this.hospitalRepository = hospitalRepository;
		this.businessRegistrationRepository = businessRegistrationRepository;
		this.categoryRepository = categoryRepository;
		this.doctorRepository = doctorRepository;
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
			case DOCTOR -> MediaCollectionPolicy.DOCTOR_PROFILE_IMAGE.equals(media.collection())
				&& doctorRepository.findByIdAndDeletedAtIsNullAndHospital_DeletedAtIsNull(media.ownerId())
					.filter(doctor -> doctor.status() == DoctorStatus.VISIBLE)
					.filter(doctor -> doctor.allowStatus() == DoctorAllowStatus.APPROVED)
					.filter(doctor -> doctor.hospital().status() == HospitalStatus.ACTIVE)
					.filter(doctor -> doctor.hospital().allowStatus() == HospitalAllowStatus.APPROVED)
					.isPresent();
			case HOSPITAL, HOSPITAL_BUSINESS_REGISTRATION -> false;
		};
		if (!readable) {
			throw new ApiException(ErrorCode.NOT_FOUND, "공개된 미디어를 찾을 수 없습니다.");
		}
	}

	private void requireActiveOwner(MediaOwnerType ownerType, Long ownerId) {
		boolean exists = switch (ownerType) {
			case HOSPITAL -> hospitalRepository.existsByIdAndDeletedAtIsNull(ownerId);
			case HOSPITAL_BUSINESS_REGISTRATION -> businessRegistrationRepository
				.existsByIdAndHospital_DeletedAtIsNull(ownerId);
			case CATEGORY -> categoryRepository.existsById(ownerId);
			case DOCTOR -> doctorRepository.existsByIdAndDeletedAtIsNullAndHospital_DeletedAtIsNull(ownerId);
		};
		if (!exists) {
			throw new ApiException(ErrorCode.NOT_FOUND, "미디어 연결 대상을 찾을 수 없습니다.");
		}
	}

	private String readPermission(MediaOwnerType ownerType) {
		return switch (ownerType) {
			case HOSPITAL, HOSPITAL_BUSINESS_REGISTRATION -> AccessPermissions.HOSPITAL_SHOW;
			case CATEGORY -> AccessPermissions.CATEGORY_MANAGE;
			case DOCTOR -> AccessPermissions.DOCTOR_SHOW;
		};
	}

}
