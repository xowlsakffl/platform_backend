package com.medi.application.specialist;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.medi.application.specialist.command.SaveSpecialistCommand;
import com.medi.application.specialist.command.UpdateSpecialistForStaffCommand;
import com.medi.application.specialist.command.UpdateSpecialistForPartnerCommand;
import com.medi.application.media.MediaCollectionPolicy;
import com.medi.application.media.MediaCommandService;
import com.medi.application.media.MediaReadService;
import com.medi.application.media.result.MediaResult;
import com.medi.common.error.ApiException;
import com.medi.common.error.ErrorCode;
import com.medi.domain.category.Category;
import com.medi.domain.category.CategoryAssignment;
import com.medi.domain.category.CategoryAssignmentTarget;
import com.medi.domain.specialist.Specialist;
import com.medi.domain.specialist.SpecialistAllowStatus;
import com.medi.domain.specialist.SpecialistStatus;
import com.medi.domain.partner.Partner;
import com.medi.domain.media.MediaOwnerType;
import com.medi.infrastructure.persistence.category.CategoryAssignmentRepository;
import com.medi.infrastructure.persistence.category.CategoryRepository;
import com.medi.infrastructure.persistence.specialist.SpecialistRepository;
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
public class SpecialistWriteService {

	private static final int MAX_CATEGORY_COUNT = 5;
	private static final int MAX_TEXT_ITEM_COUNT = 20;
	private static final int MAX_TEXT_ITEM_LENGTH = 1_000;
	private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {
	};

	private final SpecialistRepository specialistRepository;
	private final CategoryRepository categoryRepository;
	private final CategoryAssignmentRepository categoryAssignmentRepository;
	private final MediaCommandService mediaCommandService;
	private final MediaReadService mediaReadService;
	private final ObjectMapper objectMapper;

	public SpecialistWriteService(
		SpecialistRepository specialistRepository,
		CategoryRepository categoryRepository,
		CategoryAssignmentRepository categoryAssignmentRepository,
		MediaCommandService mediaCommandService,
		MediaReadService mediaReadService,
		ObjectMapper objectMapper
	) {
		this.specialistRepository = specialistRepository;
		this.categoryRepository = categoryRepository;
		this.categoryAssignmentRepository = categoryAssignmentRepository;
		this.mediaCommandService = mediaCommandService;
		this.mediaReadService = mediaReadService;
		this.objectMapper = objectMapper;
	}

	@Transactional(propagation = Propagation.MANDATORY)
	public Specialist create(Partner partner, SaveSpecialistCommand command) {
		String licenseNumber = normalizeLicenseNumber(command.licenseNumber());
		ensureLicenseAvailable(licenseNumber, null);
		SpecialistValues values = validateValues(command);

		Specialist saved = specialistRepository.saveAndFlush(new Specialist(
			partner,
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
			command.status() == null ? SpecialistStatus.HIDDEN : command.status(),
			command.allowStatus() == null ? SpecialistAllowStatus.PENDING : command.allowStatus()
		));
		syncCategories(saved.id(), command.categoryIds());
		syncMedia(saved.id(), command, true);
		return saved;
	}

	@Transactional(propagation = Propagation.MANDATORY)
	public Specialist update(Specialist specialist, Partner partner, SaveSpecialistCommand command) {
		String licenseNumber = normalizeLicenseNumber(command.licenseNumber());
		ensureLicenseAvailable(licenseNumber, specialist.id());
		SpecialistValues values = validateValues(command);

		specialist.update(
			partner,
			command.sortOrder() == null ? specialist.sortOrder() : command.sortOrder(),
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
		syncCategories(specialist.id(), command.categoryIds());
		syncMedia(specialist.id(), command, false);
		return specialistRepository.saveAndFlush(specialist);
	}

	@Transactional(propagation = Propagation.MANDATORY)
	public Specialist updatePartial(Specialist specialist, Partner partner, UpdateSpecialistForStaffCommand command) {
		SaveSpecialistCommand merged = new SaveSpecialistCommand(
			partner.id(),
			command.specified("sort_order") && command.sortOrder() != null ? command.sortOrder() : specialist.sortOrder(),
			command.specified("name") ? command.name() : specialist.name(),
			command.specified("gender") ? command.gender() : specialist.gender(),
			command.specified("position") ? command.position() : specialist.position(),
			command.specified("career_started_at") ? command.careerStartedAt() : specialist.careerStartedAt(),
			command.specified("license_number") ? command.licenseNumber() : specialist.licenseNumber(),
			command.specified("specialist_field") ? command.specialistField() : specialist.specialistField(),
			command.specified("status") ? command.status() : specialist.status(),
			command.specified("allow_status") ? command.allowStatus() : specialist.allowStatus(),
			command.specified("category_ids") ? command.categoryIds() : currentCategoryIds(specialist.id()),
			command.specified("educations") ? command.educations() : specialist.educations(),
			command.specified("careers") ? command.careers() : specialist.careers(),
			command.specified("etc_contents") ? command.etcContents() : specialist.etcContents(),
			command.profileImage(),
			resolveExistingMediaId(
				specialist.id(),
				MediaCollectionPolicy.SPECIALIST_PROFILE_IMAGE,
				command.specified("existing_profile_image_id"),
				command.existingProfileImageId(),
				command.profileImage() != null
			),
			command.licenseImage(),
			resolveExistingMediaId(
				specialist.id(),
				MediaCollectionPolicy.SPECIALIST_LICENSE_IMAGE,
				command.specified("existing_license_image_id"),
				command.existingLicenseImageId(),
				command.licenseImage() != null
			),
			command.specialistCertificateImage(),
			resolveExistingMediaId(
				specialist.id(),
				MediaCollectionPolicy.SPECIALIST_SPECIALIST_CERTIFICATE_IMAGE,
				command.specified("existing_specialist_certificate_image_id"),
				command.existingSpecialistCertificateImageId(),
				command.specialistCertificateImage() != null
			)
		);
		return update(specialist, partner, merged);
	}

	@Transactional(propagation = Propagation.MANDATORY)
	public Specialist updatePartial(Specialist specialist, Partner partner, UpdateSpecialistForPartnerCommand command) {
		SaveSpecialistCommand merged = new SaveSpecialistCommand(
			partner.id(),
			command.specified("sort_order") && command.sortOrder() != null ? command.sortOrder() : specialist.sortOrder(),
			command.specified("name") ? command.name() : specialist.name(),
			command.specified("gender") ? command.gender() : specialist.gender(),
			command.specified("position") ? command.position() : specialist.position(),
			command.specified("career_started_at") ? command.careerStartedAt() : specialist.careerStartedAt(),
			command.specified("license_number") ? command.licenseNumber() : specialist.licenseNumber(),
			command.specified("specialist_field") ? command.specialistField() : specialist.specialistField(),
			command.specified("status") ? command.status() : specialist.status(),
			specialist.allowStatus(),
			command.specified("category_ids") ? command.categoryIds() : currentCategoryIds(specialist.id()),
			command.specified("educations") ? command.educations() : specialist.educations(),
			command.specified("careers") ? command.careers() : specialist.careers(),
			command.specified("etc_contents") ? command.etcContents() : specialist.etcContents(),
			command.profileImage(),
			resolveExistingMediaId(
				specialist.id(),
				MediaCollectionPolicy.SPECIALIST_PROFILE_IMAGE,
				command.specified("existing_profile_image_id"),
				command.existingProfileImageId(),
				command.profileImage() != null
			),
			command.licenseImage(),
			resolveExistingMediaId(
				specialist.id(),
				MediaCollectionPolicy.SPECIALIST_LICENSE_IMAGE,
				command.specified("existing_license_image_id"),
				command.existingLicenseImageId(),
				command.licenseImage() != null
			),
			command.specialistCertificateImage(),
			resolveExistingMediaId(
				specialist.id(),
				MediaCollectionPolicy.SPECIALIST_SPECIALIST_CERTIFICATE_IMAGE,
				command.specified("existing_specialist_certificate_image_id"),
				command.existingSpecialistCertificateImageId(),
				command.specialistCertificateImage() != null
			)
		);
		return update(specialist, partner, merged);
	}

	private SpecialistValues validateValues(SaveSpecialistCommand command) {
		if (command.specialistField() == null) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "스페셜리스트 분야는 필수입니다.");
		}
		String name = required(command.name(), "스페셜리스트명은 필수입니다.");
		String gender = normalizeGender(command.gender());
		if (gender != null && !Set.of("남", "여").contains(gender)) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "성별 값이 올바르지 않습니다.");
		}
		String position = trimToNull(command.position());
		if (name.length() > 255 || (position != null && position.length() > 50)) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "스페셜리스트명 또는 직책 길이가 제한을 초과했습니다.");
		}
		return new SpecialistValues(
			name,
			gender,
			position,
			toJsonList(command.educations(), "학력사항"),
			toJsonList(command.careers(), "경력사항"),
			toJsonList(command.etcContents(), "활동사항")
		);
	}

	private void syncCategories(Long specialistId, List<Long> categoryIds) {
		List<Long> normalizedIds = categoryIds == null
			? List.of()
			: categoryIds.stream().filter(Objects::nonNull).distinct().toList();
		categoryAssignmentRepository.deleteByCategorizableTypeAndCategorizableId(
			CategoryAssignment.SPECIALIST_TARGET_TYPE,
			specialistId
		);
		categoryAssignmentRepository.flush();
		if (normalizedIds.isEmpty()) {
			return;
		}
		if (normalizedIds.size() > MAX_CATEGORY_COUNT) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "시술분야는 최대 5개까지 선택할 수 있습니다.");
		}
		List<Category> categories = categoryRepository.findByIdIn(normalizedIds);
		Map<Long, Category> categoryMap = categories.stream().collect(Collectors.toMap(Category::id, Function.identity()));
		boolean invalid = categories.size() != normalizedIds.size()
			|| categories.stream().anyMatch(category -> !CategoryAssignmentTarget.SPECIALIST.accepts(category));
		if (invalid) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "선택한 시술분야 카테고리가 올바르지 않습니다.");
		}

		List<CategoryAssignment> assignments = new ArrayList<>();
		for (int index = 0; index < normalizedIds.size(); index++) {
			assignments.add(new CategoryAssignment(
				CategoryAssignment.SPECIALIST_TARGET_TYPE,
				specialistId,
				categoryMap.get(normalizedIds.get(index)),
				index == 0
			));
		}
		categoryAssignmentRepository.saveAll(assignments);
	}

	private void syncMedia(
		Long specialistId,
		SaveSpecialistCommand command,
		boolean creating
	) {
		mediaCommandService.synchronizeSingle(
			MediaOwnerType.SPECIALIST,
			specialistId,
			MediaCollectionPolicy.SPECIALIST_PROFILE_IMAGE,
			command.profileImage(),
			creating ? null : command.existingProfileImageId(),
			false
		);
		mediaCommandService.synchronizeSingle(
			MediaOwnerType.SPECIALIST,
			specialistId,
			MediaCollectionPolicy.SPECIALIST_LICENSE_IMAGE,
			command.licenseImage(),
			creating ? null : command.existingLicenseImageId(),
			false
		);
		mediaCommandService.synchronizeSingle(
			MediaOwnerType.SPECIALIST,
			specialistId,
			MediaCollectionPolicy.SPECIALIST_SPECIALIST_CERTIFICATE_IMAGE,
			command.specialistCertificateImage(),
			creating ? null : command.existingSpecialistCertificateImageId(),
			false
		);
	}

	private void ensureLicenseAvailable(String licenseNumber, Long excludedId) {
		if (!StringUtils.hasText(licenseNumber)) {
			return;
		}
		boolean exists = excludedId == null
			? specialistRepository.existsByLicenseNumber(licenseNumber)
			: specialistRepository.existsByLicenseNumberAndIdNot(licenseNumber, excludedId);
		if (exists) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "이미 등록된 자격 증빙 번호입니다.");
		}
	}

	private String normalizeLicenseNumber(String licenseNumber) {
		String normalized = licenseNumber == null ? "" : licenseNumber.replaceAll("\\D", "");
		return normalized.isEmpty() ? null : normalized;
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

	private List<Long> currentCategoryIds(Long specialistId) {
		return categoryAssignmentRepository
			.findByCategorizableTypeAndCategorizableId(CategoryAssignment.SPECIALIST_TARGET_TYPE, specialistId)
			.stream()
			.sorted(Comparator.comparing((CategoryAssignment assignment) -> !assignment.primary())
				.thenComparing(assignment -> assignment.category().id()))
			.map(assignment -> assignment.category().id())
			.toList();
	}

	private Long resolveExistingMediaId(
		Long specialistId,
		String collection,
		boolean existingFieldSpecified,
		Long requestedExistingId,
		boolean newFileSpecified
	) {
		if (newFileSpecified || existingFieldSpecified) {
			return requestedExistingId;
		}
		MediaResult current = mediaReadService.primary(MediaOwnerType.SPECIALIST, specialistId, collection);
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

	private record SpecialistValues(
		String name,
		String gender,
		String position,
		String educationsJson,
		String careersJson,
		String etcContentsJson
	) {
	}
}
