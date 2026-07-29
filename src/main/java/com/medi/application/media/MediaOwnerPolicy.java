package com.medi.application.media;

import com.medi.application.auth.PermissionService;
import com.medi.common.error.ApiException;
import com.medi.common.error.ErrorCode;
import com.medi.common.security.AuthenticatedActor;
import com.medi.domain.media.MediaOwnerType;
import com.medi.domain.doctor.Doctor;
import com.medi.infrastructure.persistence.category.CategoryRepository;
import com.medi.infrastructure.persistence.doctor.DoctorRepository;
import com.medi.infrastructure.persistence.hospital.HospitalRepository;
import org.springframework.stereotype.Component;

@Component
class MediaOwnerPolicy {

	private final PermissionService permissionService;
	private final HospitalRepository hospitalRepository;
	private final CategoryRepository categoryRepository;
	private final DoctorRepository doctorRepository;

	MediaOwnerPolicy(
		PermissionService permissionService,
		HospitalRepository hospitalRepository,
		CategoryRepository categoryRepository,
		DoctorRepository doctorRepository
	) {
		this.permissionService = permissionService;
		this.hospitalRepository = hospitalRepository;
		this.categoryRepository = categoryRepository;
		this.doctorRepository = doctorRepository;
	}

	void requireReadable(AuthenticatedActor actor, MediaOwnerType ownerType, Long ownerId) {
		permissionService.requireStaffPermission(actor, readPermission(ownerType));
		requireActiveOwner(ownerType, ownerId);
	}

	void requireMutable(AuthenticatedActor actor, MediaOwnerType ownerType, Long ownerId) {
		permissionService.requireStaffPermission(actor, updatePermission(ownerType));
		requireMutableOwner(ownerType, ownerId);
	}

	private void requireMutableOwner(MediaOwnerType ownerType, Long ownerId) {
		boolean exists = switch (ownerType) {
			case HOSPITAL -> hospitalRepository.findForUpdateByIdAndDeletedAtIsNull(ownerId).isPresent();
			case CATEGORY -> categoryRepository.findForUpdateById(ownerId).isPresent();
			case DOCTOR -> lockActiveDoctor(ownerId);
		};
		if (!exists) {
			throw new ApiException(ErrorCode.NOT_FOUND, "미디어 연결 대상을 찾을 수 없습니다.");
		}
	}

	private void requireActiveOwner(MediaOwnerType ownerType, Long ownerId) {
		boolean exists = switch (ownerType) {
			case HOSPITAL -> hospitalRepository.existsByIdAndDeletedAtIsNull(ownerId);
			case CATEGORY -> categoryRepository.existsById(ownerId);
			case DOCTOR -> doctorRepository.existsByIdAndDeletedAtIsNullAndHospital_DeletedAtIsNull(ownerId);
		};
		if (!exists) {
			throw new ApiException(ErrorCode.NOT_FOUND, "미디어 연결 대상을 찾을 수 없습니다.");
		}
	}

	private String readPermission(MediaOwnerType ownerType) {
		return switch (ownerType) {
			case HOSPITAL -> "platform.hospital.show";
			case CATEGORY -> "platform.category.manage";
			case DOCTOR -> "platform.doctor.show";
		};
	}

	private String updatePermission(MediaOwnerType ownerType) {
		return switch (ownerType) {
			case HOSPITAL -> "platform.hospital.update";
			case CATEGORY -> "platform.category.manage";
			case DOCTOR -> "platform.doctor.update";
		};
	}

	private boolean lockActiveDoctor(Long doctorId) {
		Doctor reference = doctorRepository.findByIdAndDeletedAtIsNullAndHospital_DeletedAtIsNull(doctorId).orElse(null);
		if (reference == null || hospitalRepository.findForUpdateByIdAndDeletedAtIsNull(reference.hospitalId()).isEmpty()) {
			return false;
		}
		return doctorRepository.findForUpdateByIdAndDeletedAtIsNull(doctorId).isPresent();
	}
}
