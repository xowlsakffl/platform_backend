package com.medi.application.doctor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.medi.application.doctor.command.SaveDoctorCommand;
import com.medi.application.doctor.command.UpdateDoctorForStaffCommand;
import com.medi.application.doctor.command.UpdateDoctorForHospitalCommand;
import com.medi.application.media.MediaCollectionPolicy;
import com.medi.application.media.MediaCommandService;
import com.medi.application.media.MediaReadService;
import com.medi.application.media.result.MediaResult;
import com.medi.common.error.ApiException;
import com.medi.common.error.ErrorCode;
import com.medi.domain.category.Category;
import com.medi.domain.category.CategoryAssignment;
import com.medi.domain.category.CategoryAssignmentTarget;
import com.medi.domain.doctor.Doctor;
import com.medi.domain.doctor.DoctorAllowStatus;
import com.medi.domain.doctor.DoctorStatus;
import com.medi.domain.hospital.Hospital;
import com.medi.domain.media.MediaOwnerType;
import com.medi.infrastructure.persistence.category.CategoryAssignmentRepository;
import com.medi.infrastructure.persistence.category.CategoryRepository;
import com.medi.infrastructure.persistence.doctor.DoctorRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class DoctorWriteService {

	private static final int MAX_CATEGORY_COUNT = 5;
	private static final int MAX_TEXT_ITEM_COUNT = 20;
	private static final int MAX_TEXT_ITEM_LENGTH = 1_000;
	private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {
	};

	private final DoctorRepository doctorRepository;
	private final CategoryRepository categoryRepository;
	private final CategoryAssignmentRepository categoryAssignmentRepository;
	private final MediaCommandService mediaCommandService;
	private final MediaReadService mediaReadService;
	private final ObjectMapper objectMapper;

	public DoctorWriteService(
		DoctorRepository doctorRepository,
		CategoryRepository categoryRepository,
		CategoryAssignmentRepository categoryAssignmentRepository,
		MediaCommandService mediaCommandService,
		MediaReadService mediaReadService,
		ObjectMapper objectMapper
	) {
		this.doctorRepository = doctorRepository;
		this.categoryRepository = categoryRepository;
		this.categoryAssignmentRepository = categoryAssignmentRepository;
		this.mediaCommandService = mediaCommandService;
		this.mediaReadService = mediaReadService;
		this.objectMapper = objectMapper;
	}

	@Transactional(propagation = Propagation.MANDATORY)
	public Doctor create(Hospital hospital, SaveDoctorCommand command) {
		String licenseNumber = normalizeRequiredLicenseNumber(command.licenseNumber());
		ensureLicenseAvailable(licenseNumber, null);
		DoctorValues values = validateValues(command);

		Doctor saved = doctorRepository.saveAndFlush(new Doctor(
			hospital,
			command.sortOrder() == null ? 0 : command.sortOrder(),
			values.name(),
			values.gender(),
			values.position(),
			command.careerStartedAt(),
			licenseNumber,
			command.specialistField(),
			values.educationsJson(),
			values.careersJson(),
			values.etcContentsJson(),
			command.status() == null ? DoctorStatus.HIDDEN : command.status(),
			command.allowStatus() == null ? DoctorAllowStatus.PENDING : command.allowStatus()
		));
		syncCategories(saved.id(), command.categoryIds());
		syncMedia(saved.id(), command, true);
		return saved;
	}

	@Transactional(propagation = Propagation.MANDATORY)
	public Doctor update(Doctor doctor, Hospital hospital, SaveDoctorCommand command) {
		String licenseNumber = normalizeRequiredLicenseNumber(command.licenseNumber());
		ensureLicenseAvailable(licenseNumber, doctor.id());
		DoctorValues values = validateValues(command);

		doctor.update(
			hospital,
			command.sortOrder() == null ? doctor.sortOrder() : command.sortOrder(),
			values.name(),
			values.gender(),
			values.position(),
			command.careerStartedAt(),
			licenseNumber,
			command.specialistField(),
			values.educationsJson(),
			values.careersJson(),
			values.etcContentsJson(),
			command.status(),
			command.allowStatus()
		);
		syncCategories(doctor.id(), command.categoryIds());
		syncMedia(doctor.id(), command, false);
		return doctorRepository.saveAndFlush(doctor);
	}

	@Transactional(propagation = Propagation.MANDATORY)
	public Doctor updatePartial(Doctor doctor, Hospital hospital, UpdateDoctorForStaffCommand command) {
		SaveDoctorCommand merged = new SaveDoctorCommand(
			hospital.id(),
			command.specified("sort_order") && command.sortOrder() != null ? command.sortOrder() : doctor.sortOrder(),
			command.specified("name") ? command.name() : doctor.name(),
			command.specified("gender") ? command.gender() : doctor.gender(),
			command.specified("position") ? command.position() : doctor.position(),
			command.specified("career_started_at") ? command.careerStartedAt() : doctor.careerStartedAt(),
			command.specified("license_number") ? command.licenseNumber() : doctor.licenseNumber(),
			command.specified("specialist_field") ? command.specialistField() : doctor.specialistField(),
			command.specified("status") ? command.status() : doctor.status(),
			command.specified("allow_status") ? command.allowStatus() : doctor.allowStatus(),
			command.specified("category_ids") ? command.categoryIds() : currentCategoryIds(doctor.id()),
			command.specified("educations") ? command.educations() : doctor.educations(),
			command.specified("careers") ? command.careers() : doctor.careers(),
			command.specified("etc_contents") ? command.etcContents() : doctor.etcContents(),
			command.profileImage(),
			resolveExistingMediaId(
				doctor.id(),
				MediaCollectionPolicy.DOCTOR_PROFILE_IMAGE,
				command.specified("existing_profile_image_id"),
				command.existingProfileImageId(),
				command.profileImage() != null
			),
			command.licenseImage(),
			resolveExistingMediaId(
				doctor.id(),
				MediaCollectionPolicy.DOCTOR_LICENSE_IMAGE,
				command.specified("existing_license_image_id"),
				command.existingLicenseImageId(),
				command.licenseImage() != null
			),
			command.specialistCertificateImage(),
			resolveExistingMediaId(
				doctor.id(),
				MediaCollectionPolicy.DOCTOR_SPECIALIST_CERTIFICATE_IMAGE,
				command.specified("existing_specialist_certificate_image_id"),
				command.existingSpecialistCertificateImageId(),
				command.specialistCertificateImage() != null
			)
		);
		return update(doctor, hospital, merged);
	}

	@Transactional(propagation = Propagation.MANDATORY)
	public Doctor updatePartial(Doctor doctor, Hospital hospital, UpdateDoctorForHospitalCommand command) {
		SaveDoctorCommand merged = new SaveDoctorCommand(
			hospital.id(),
			command.specified("sort_order") && command.sortOrder() != null ? command.sortOrder() : doctor.sortOrder(),
			command.specified("name") ? command.name() : doctor.name(),
			command.specified("gender") ? command.gender() : doctor.gender(),
			command.specified("position") ? command.position() : doctor.position(),
			command.specified("career_started_at") ? command.careerStartedAt() : doctor.careerStartedAt(),
			command.specified("license_number") ? command.licenseNumber() : doctor.licenseNumber(),
			command.specified("specialist_field") ? command.specialistField() : doctor.specialistField(),
			command.specified("status") ? command.status() : doctor.status(),
			doctor.allowStatus(),
			command.specified("category_ids") ? command.categoryIds() : currentCategoryIds(doctor.id()),
			command.specified("educations") ? command.educations() : doctor.educations(),
			command.specified("careers") ? command.careers() : doctor.careers(),
			command.specified("etc_contents") ? command.etcContents() : doctor.etcContents(),
			command.profileImage(),
			resolveExistingMediaId(
				doctor.id(),
				MediaCollectionPolicy.DOCTOR_PROFILE_IMAGE,
				command.specified("existing_profile_image_id"),
				command.existingProfileImageId(),
				command.profileImage() != null
			),
			command.licenseImage(),
			resolveExistingMediaId(
				doctor.id(),
				MediaCollectionPolicy.DOCTOR_LICENSE_IMAGE,
				command.specified("existing_license_image_id"),
				command.existingLicenseImageId(),
				command.licenseImage() != null
			),
			command.specialistCertificateImage(),
			resolveExistingMediaId(
				doctor.id(),
				MediaCollectionPolicy.DOCTOR_SPECIALIST_CERTIFICATE_IMAGE,
				command.specified("existing_specialist_certificate_image_id"),
				command.existingSpecialistCertificateImageId(),
				command.specialistCertificateImage() != null
			)
		);
		return update(doctor, hospital, merged);
	}

	private DoctorValues validateValues(SaveDoctorCommand command) {
		if (command.specialistField() == null) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "전문의 분류는 필수입니다.");
		}
		String name = required(command.name(), "의료진명은 필수입니다.");
		String gender = normalizeGender(command.gender());
		if (gender != null && !Set.of("남", "여").contains(gender)) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "성별 값이 올바르지 않습니다.");
		}
		String position = trimToNull(command.position());
		if (position != null && !Set.of("대표원장", "원장").contains(position)) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "직책 값이 올바르지 않습니다.");
		}
		if (name.length() > 255) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "의료진명 또는 직책 길이가 제한을 초과했습니다.");
		}
		return new DoctorValues(
			name,
			gender,
			position,
			toJsonList(command.educations(), "학력사항"),
			toJsonList(command.careers(), "경력사항"),
			toJsonList(command.etcContents(), "활동사항")
		);
	}

	private void syncCategories(Long doctorId, List<Long> categoryIds) {
		List<Long> normalizedIds = categoryIds == null
			? List.of()
			: categoryIds.stream().filter(Objects::nonNull).distinct().toList();
		categoryAssignmentRepository.deleteByCategorizableTypeAndCategorizableId(
			CategoryAssignment.DOCTOR_TARGET_TYPE,
			doctorId
		);
		categoryAssignmentRepository.flush();
		if (normalizedIds.isEmpty()) {
			return;
		}
		if (normalizedIds.size() > MAX_CATEGORY_COUNT) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "진료분야는 최대 5개까지 선택할 수 있습니다.");
		}
		List<Category> categories = categoryRepository.findByIdIn(normalizedIds);
		Map<Long, Category> categoryMap = categories.stream().collect(Collectors.toMap(Category::id, Function.identity()));
		boolean invalid = categories.size() != normalizedIds.size()
			|| categories.stream().anyMatch(category -> !CategoryAssignmentTarget.DOCTOR.accepts(category));
		if (invalid) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "선택한 진료분야 카테고리가 올바르지 않습니다.");
		}

		List<CategoryAssignment> assignments = new ArrayList<>();
		for (int index = 0; index < normalizedIds.size(); index++) {
			assignments.add(new CategoryAssignment(
				CategoryAssignment.DOCTOR_TARGET_TYPE,
				doctorId,
				categoryMap.get(normalizedIds.get(index)),
				index == 0
			));
		}
		categoryAssignmentRepository.saveAll(assignments);
	}

	private void syncMedia(
		Long doctorId,
		SaveDoctorCommand command,
		boolean creating
	) {
		mediaCommandService.synchronizeSingle(
			MediaOwnerType.DOCTOR,
			doctorId,
			MediaCollectionPolicy.DOCTOR_PROFILE_IMAGE,
			command.profileImage(),
			creating ? null : command.existingProfileImageId(),
			false
		);
		mediaCommandService.synchronizeSingle(
			MediaOwnerType.DOCTOR,
			doctorId,
			MediaCollectionPolicy.DOCTOR_LICENSE_IMAGE,
			command.licenseImage(),
			creating ? null : command.existingLicenseImageId(),
			false
		);
		mediaCommandService.synchronizeSingle(
			MediaOwnerType.DOCTOR,
			doctorId,
			MediaCollectionPolicy.DOCTOR_SPECIALIST_CERTIFICATE_IMAGE,
			command.specialistCertificateImage(),
			creating ? null : command.existingSpecialistCertificateImageId(),
			false
		);
	}

	private void ensureLicenseAvailable(String licenseNumber, Long excludedId) {
		boolean exists = excludedId == null
			? doctorRepository.existsByLicenseNumber(licenseNumber)
			: doctorRepository.existsByLicenseNumberAndIdNot(licenseNumber, excludedId);
		if (exists) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "이미 등록된 의사면허 번호입니다.");
		}
	}

	private String normalizeRequiredLicenseNumber(String licenseNumber) {
		String normalized = licenseNumber == null ? "" : licenseNumber.replaceAll("\\D", "");
		if (normalized.isEmpty()) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "의사면허 번호는 필수입니다.");
		}
		return normalized;
	}

	private String toJsonList(String raw, String fieldName) {
		if (!StringUtils.hasText(raw)) {
			return "[]";
		}
		try {
			List<String> values = objectMapper.readValue(raw, STRING_LIST_TYPE).stream()
				.map(String::trim)
				.filter(value -> !value.isEmpty())
				.toList();
			if (values.size() > MAX_TEXT_ITEM_COUNT || values.stream().anyMatch(value -> value.length() > MAX_TEXT_ITEM_LENGTH)) {
				throw new ApiException(ErrorCode.INVALID_REQUEST, fieldName + " 입력 개수 또는 길이가 제한을 초과했습니다.");
			}
			return objectMapper.writeValueAsString(values);
		} catch (JsonProcessingException | NullPointerException exception) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, fieldName + " JSON 배열이 올바르지 않습니다.");
		}
	}

	private String required(String value, String message) {
		if (!StringUtils.hasText(value)) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, message);
		}
		return value.trim();
	}

	private List<Long> currentCategoryIds(Long doctorId) {
		return categoryAssignmentRepository
			.findByCategorizableTypeAndCategorizableId(CategoryAssignment.DOCTOR_TARGET_TYPE, doctorId)
			.stream()
			.sorted(Comparator.comparing((CategoryAssignment assignment) -> !assignment.primary())
				.thenComparing(assignment -> assignment.category().id()))
			.map(assignment -> assignment.category().id())
			.toList();
	}

	private Long resolveExistingMediaId(
		Long doctorId,
		String collection,
		boolean existingFieldSpecified,
		Long requestedExistingId,
		boolean newFileSpecified
	) {
		if (newFileSpecified || existingFieldSpecified) {
			return requestedExistingId;
		}
		MediaResult current = mediaReadService.primary(MediaOwnerType.DOCTOR, doctorId, collection);
		return current == null ? null : current.id();
	}

	private String normalizeGender(String value) {
		String normalized = trimToNull(value);
		if (normalized == null) {
			return null;
		}
		return switch (normalized.toUpperCase(Locale.ROOT)) {
			case "M", "MALE", "MAN", "남" -> "남";
			case "F", "FEMALE", "WOMAN", "여" -> "여";
			default -> normalized;
		};
	}

	private String trimToNull(String value) {
		return StringUtils.hasText(value) ? value.trim() : null;
	}

	private record DoctorValues(
		String name,
		String gender,
		String position,
		String educationsJson,
		String careersJson,
		String etcContentsJson
	) {
	}
}
