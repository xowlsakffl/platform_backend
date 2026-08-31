package com.platform.application.specialist;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.application.media.MediaCollectionPolicy;
import com.platform.application.media.MediaCommandService;
import com.platform.application.partner.PartnerSchedulePolicyValidator;
import com.platform.application.specialist.command.SaveSpecialistCommand;
import com.platform.application.specialist.command.UpdateSpecialistForPartnerCommand;
import com.platform.application.specialist.command.UpdateSpecialistForStaffCommand;
import com.platform.common.error.ApiException;
import com.platform.common.error.ErrorCode;
import com.platform.domain.media.MediaOwnerType;
import com.platform.domain.partner.Partner;
import com.platform.domain.partner.PartnerOption;
import com.platform.domain.specialist.Specialist;
import com.platform.domain.specialist.SpecialistAllowStatus;
import com.platform.domain.specialist.SpecialistField;
import com.platform.domain.specialist.SpecialistOption;
import com.platform.domain.specialist.SpecialistScheduleMode;
import com.platform.domain.specialist.SpecialistStatus;
import com.platform.infrastructure.persistence.partner.PartnerOptionRepository;
import com.platform.infrastructure.persistence.specialist.SpecialistOptionRepository;
import com.platform.infrastructure.persistence.specialist.SpecialistRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class SpecialistWriteService {

	private static final int MAX_OPTION_ASSIGNMENT_COUNT = 300;
	private static final TypeReference<List<OptionAssignmentValue>> OPTION_ASSIGNMENT_LIST_TYPE = new TypeReference<>() {
	};

	private final SpecialistRepository specialistRepository;
	private final SpecialistOptionRepository specialistOptionRepository;
	private final PartnerOptionRepository partnerOptionRepository;
	private final MediaCommandService mediaCommandService;
	private final PartnerSchedulePolicyValidator schedulePolicyValidator;
	private final ObjectMapper objectMapper;

	public SpecialistWriteService(
		SpecialistRepository specialistRepository,
		SpecialistOptionRepository specialistOptionRepository,
		PartnerOptionRepository partnerOptionRepository,
		MediaCommandService mediaCommandService,
		PartnerSchedulePolicyValidator schedulePolicyValidator,
		ObjectMapper objectMapper
	) {
		this.specialistRepository = specialistRepository;
		this.specialistOptionRepository = specialistOptionRepository;
		this.partnerOptionRepository = partnerOptionRepository;
		this.mediaCommandService = mediaCommandService;
		this.schedulePolicyValidator = schedulePolicyValidator;
		this.objectMapper = objectMapper;
	}

	@Transactional(propagation = Propagation.MANDATORY)
	public Specialist create(Partner partner, SaveSpecialistCommand command) {
		SpecialistValues values = validateValues(partner, command);
		List<ResolvedOptionAssignment> optionAssignments = resolveOptionAssignments(
			partner.id(),
			command.optionAssignments()
		);

		Specialist saved = specialistRepository.saveAndFlush(new Specialist(
			partner,
			specialistRepository.nextSortOrder(partner.id()),
			values.name(),
			values.gender(),
			values.position(),
			command.careerStartedAt(),
			values.specialistField(),
			values.introduction(),
			values.scheduleMode(),
			values.operationHoursJson(),
			values.holidayPolicyJson(),
			command.status() == null ? SpecialistStatus.HIDDEN : command.status(),
			command.allowStatus() == null ? SpecialistAllowStatus.REVIEW_REQUESTED : command.allowStatus()
		));
		replaceOptionAssignments(saved, optionAssignments);
		syncMedia(saved.id(), command, true);
		return saved;
	}

	@Transactional(propagation = Propagation.MANDATORY)
	public Specialist updatePartial(Specialist specialist, Partner partner, UpdateSpecialistForStaffCommand command) {
		SaveSpecialistCommand merged = new SaveSpecialistCommand(
			partner.id(),
			command.specified("name") ? command.name() : specialist.name(),
			command.specified("gender") ? command.gender() : specialist.gender(),
			command.specified("position") ? command.position() : specialist.position(),
			command.specified("career_started_at") ? command.careerStartedAt() : specialist.careerStartedAt(),
			command.specified("specialist_field") ? command.specialistField() : specialist.specialistField(),
			command.specified("introduction") ? command.introduction() : specialist.introduction(),
			command.specified("schedule_mode") ? command.scheduleMode() : specialist.scheduleMode(),
			command.specified("operation_hours") ? command.operationHours() : specialist.operationHours(),
			command.specified("holiday_policy") ? command.holidayPolicy() : specialist.holidayPolicy(),
			command.specified("status") ? command.status() : specialist.status(),
			command.specified("allow_status") ? command.allowStatus() : specialist.allowStatus(),
			command.optionAssignments(),
			command.profileImages(),
			command.profileImageOrder(),
			command.certificationImages(),
			command.certificationImageOrder()
		);
		Specialist saved = updateCore(specialist, partner, merged, command.specified("option_assignments"));
		syncMediaIfSpecified(
			saved.id(),
			command.specified("profile_image_files") || command.specified("profile_image_order"),
			command.profileImages(),
			command.profileImageOrder(),
			command.specified("certification_image_files") || command.specified("certification_image_order"),
			command.certificationImages(),
			command.certificationImageOrder()
		);
		return saved;
	}

	@Transactional(propagation = Propagation.MANDATORY)
	public Specialist updatePartial(Specialist specialist, Partner partner, UpdateSpecialistForPartnerCommand command) {
		SaveSpecialistCommand merged = new SaveSpecialistCommand(
			partner.id(),
			command.specified("name") ? command.name() : specialist.name(),
			command.specified("gender") ? command.gender() : specialist.gender(),
			command.specified("position") ? command.position() : specialist.position(),
			command.specified("career_started_at") ? command.careerStartedAt() : specialist.careerStartedAt(),
			command.specified("specialist_field") ? command.specialistField() : specialist.specialistField(),
			command.specified("introduction") ? command.introduction() : specialist.introduction(),
			command.specified("schedule_mode") ? command.scheduleMode() : specialist.scheduleMode(),
			command.specified("operation_hours") ? command.operationHours() : specialist.operationHours(),
			command.specified("holiday_policy") ? command.holidayPolicy() : specialist.holidayPolicy(),
			command.specified("status") ? command.status() : specialist.status(),
			specialist.allowStatus(),
			command.optionAssignments(),
			command.profileImages(),
			command.profileImageOrder(),
			command.certificationImages(),
			command.certificationImageOrder()
		);
		Specialist saved = updateCore(specialist, partner, merged, command.specified("option_assignments"));
		syncMediaIfSpecified(
			saved.id(),
			command.specified("profile_image_files") || command.specified("profile_image_order"),
			command.profileImages(),
			command.profileImageOrder(),
			command.specified("certification_image_files") || command.specified("certification_image_order"),
			command.certificationImages(),
			command.certificationImageOrder()
		);
		return saved;
	}

	private Specialist updateCore(
		Specialist specialist,
		Partner partner,
		SaveSpecialistCommand command,
		boolean replaceOptions
	) {
		SpecialistValues values = validateValues(partner, command);
		List<ResolvedOptionAssignment> optionAssignments = replaceOptions
			? resolveOptionAssignments(partner.id(), command.optionAssignments())
			: List.of();
		specialist.update(
			values.name(),
			values.gender(),
			values.position(),
			command.careerStartedAt(),
			values.specialistField(),
			values.introduction(),
			values.scheduleMode(),
			values.operationHoursJson(),
			values.holidayPolicyJson(),
			command.status(),
			command.allowStatus()
		);
		if (replaceOptions) {
			replaceOptionAssignments(specialist, optionAssignments);
		}
		return specialistRepository.saveAndFlush(specialist);
	}

	private SpecialistValues validateValues(Partner partner, SaveSpecialistCommand command) {
		if (command.careerStartedAt() != null && command.careerStartedAt().isAfter(LocalDate.now())) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "총 경력 시작일은 오늘 이후로 설정할 수 없습니다.");
		}
		SpecialistField specialistField = command.specialistField();
		if (specialistField == null) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "전문가 분야는 필수입니다.");
		}
		String name = required(command.name(), "전문가명은 필수입니다.");
		String gender = normalizeGender(command.gender());
		if (gender != null && !Set.of("남", "여").contains(gender)) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "성별 값이 올바르지 않습니다.");
		}
		String position = trimToNull(command.position());
		String introduction = trimToNull(command.introduction());
		if (name.length() > 255 || (position != null && position.length() > 50)) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "전문가명 또는 직책 길이가 제한을 초과했습니다.");
		}
		if (introduction != null && introduction.length() > 500) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "전문가 소개는 500자까지 입력할 수 있습니다.");
		}
		SpecialistScheduleMode scheduleMode = command.scheduleMode() == null
			? SpecialistScheduleMode.INHERIT_PARTNER_HOURS
			: command.scheduleMode();
		String operationHours = null;
		if (scheduleMode == SpecialistScheduleMode.CUSTOM_HOURS) {
			operationHours = schedulePolicyValidator.normalizeOperationHours(command.operationHours(), true);
			schedulePolicyValidator.assertWithinPartnerHours(operationHours, partner.operationHours());
		}
		Object holidayPolicy = command.holidayPolicy() == null ? Map.of("enabled", false) : command.holidayPolicy();
		return new SpecialistValues(
			name,
			gender,
			position,
			specialistField,
			introduction,
			scheduleMode,
			operationHours,
			schedulePolicyValidator.normalizeHolidayPolicy(holidayPolicy, true)
		);
	}

	private List<ResolvedOptionAssignment> resolveOptionAssignments(Long partnerId, String raw) {
		List<OptionAssignmentValue> values;
		try {
			values = StringUtils.hasText(raw) ? objectMapper.readValue(raw, OPTION_ASSIGNMENT_LIST_TYPE) : List.of();
		} catch (JsonProcessingException exception) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "전문가 시술 옵션 형식이 올바르지 않습니다.");
		}
		if (values.size() > MAX_OPTION_ASSIGNMENT_COUNT) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "전문가 시술 옵션은 최대 300개까지 연결할 수 있습니다.");
		}
		Set<Long> optionIds = new HashSet<>();
		for (OptionAssignmentValue value : values) {
			if (value == null || value.partnerOptionId() == null || value.partnerOptionId() <= 0
				|| !optionIds.add(value.partnerOptionId())) {
				throw new ApiException(ErrorCode.INVALID_REQUEST, "전문가 시술 옵션 선택값이 올바르지 않습니다.");
			}
			validateOverride(value);
		}
		if (optionIds.isEmpty()) {
			return List.of();
		}
		Map<Long, PartnerOption> options = new LinkedHashMap<>();
		partnerOptionRepository.findByIdInAndPartner_IdAndDeletedAtIsNull(optionIds, partnerId)
			.forEach(option -> options.put(option.id(), option));
		if (options.size() != optionIds.size()) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "해당 업체에서 사용할 수 없는 시술 옵션이 포함되어 있습니다.");
		}
		List<ResolvedOptionAssignment> resolved = new ArrayList<>();
		for (OptionAssignmentValue value : values) {
			resolved.add(new ResolvedOptionAssignment(options.get(value.partnerOptionId()), value));
		}
		return List.copyOf(resolved);
	}

	private void validateOverride(OptionAssignmentValue value) {
		BigDecimal regularPrice = value.regularPriceOverride();
		BigDecimal salePrice = value.salePriceOverride();
		if (regularPrice == null && salePrice != null) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "할인가를 다르게 설정하려면 정상가도 함께 입력해 주세요.");
		}
		if (!validPrice(regularPrice)
			|| !validPrice(salePrice)
			|| salePrice != null && salePrice.compareTo(regularPrice) >= 0) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "전문가별 가격이 올바르지 않습니다.");
		}
	}

	private boolean validPrice(BigDecimal value) {
		if (value == null) {
			return true;
		}
		return value.signum() >= 0 && value.scale() <= 2 && value.precision() - value.scale() <= 10;
	}

	private void replaceOptionAssignments(
		Specialist specialist,
		List<ResolvedOptionAssignment> assignments
	) {
		specialistOptionRepository.deleteBySpecialist_Id(specialist.id());
		specialistOptionRepository.flush();
		if (assignments.isEmpty()) {
			return;
		}
		specialistOptionRepository.saveAll(assignments.stream()
			.map(assignment -> new SpecialistOption(
				specialist,
				assignment.option(),
				assignment.value().regularPriceOverride(),
				assignment.value().salePriceOverride()
			))
			.toList());
	}

	private void syncMedia(Long specialistId, SaveSpecialistCommand command, boolean creating) {
		if (!creating) {
			return;
		}
		mediaCommandService.synchronizeManyOrdered(
			MediaOwnerType.SPECIALIST,
			specialistId,
			MediaCollectionPolicy.SPECIALIST_PROFILE_IMAGE,
			command.profileImages(),
			command.profileImageOrder(),
			false,
			3
		);
		mediaCommandService.synchronizeManyOrdered(
			MediaOwnerType.SPECIALIST,
			specialistId,
			MediaCollectionPolicy.SPECIALIST_CERTIFICATION_IMAGE,
			command.certificationImages(),
			command.certificationImageOrder(),
			false,
			5
		);
	}

	private void syncMediaIfSpecified(
		Long specialistId,
		boolean profileSpecified,
		List<com.platform.application.media.storage.MediaFileSource> profileImages,
		List<String> profileImageOrder,
		boolean certificationSpecified,
		List<com.platform.application.media.storage.MediaFileSource> certificationImages,
		List<String> certificationImageOrder
	) {
		if (profileSpecified) {
			mediaCommandService.synchronizeManyOrdered(
				MediaOwnerType.SPECIALIST,
				specialistId,
				MediaCollectionPolicy.SPECIALIST_PROFILE_IMAGE,
				profileImages,
				profileImageOrder,
				false,
				3
			);
		}
		if (certificationSpecified) {
			mediaCommandService.synchronizeManyOrdered(
				MediaOwnerType.SPECIALIST,
				specialistId,
				MediaCollectionPolicy.SPECIALIST_CERTIFICATION_IMAGE,
				certificationImages,
				certificationImageOrder,
				false,
				5
			);
		}
	}

	private String required(String value, String message) {
		if (!StringUtils.hasText(value)) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, message);
		}
		return value.trim();
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
		SpecialistField specialistField,
		String introduction,
		SpecialistScheduleMode scheduleMode,
		String operationHoursJson,
		String holidayPolicyJson
	) {
	}

	private record OptionAssignmentValue(
		@JsonProperty("partner_option_id") Long partnerOptionId,
		@JsonProperty("regular_price_override") BigDecimal regularPriceOverride,
		@JsonProperty("sale_price_override") BigDecimal salePriceOverride
	) {
	}

	private record ResolvedOptionAssignment(PartnerOption option, OptionAssignmentValue value) {
	}
}
