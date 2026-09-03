package com.platform.application.specialist;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.application.media.MediaCollectionPolicy;
import com.platform.application.media.MediaReadService;
import com.platform.application.media.result.MediaResult;
import com.platform.common.error.InternalApplicationException;
import com.platform.common.security.AuthenticatedActor;
import com.platform.domain.specialist.Specialist;
import com.platform.domain.media.MediaOwnerType;
import com.platform.domain.operationhistory.OperationHistory;
import com.platform.infrastructure.persistence.operationhistory.OperationHistoryRepository;
import com.platform.infrastructure.persistence.specialist.SpecialistOptionRepository;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SpecialistHistoryService {

	private final OperationHistoryRepository operationHistoryRepository;
	private final MediaReadService mediaReadService;
	private final SpecialistOptionRepository specialistOptionRepository;
	private final ObjectMapper objectMapper;

	public SpecialistHistoryService(
		OperationHistoryRepository operationHistoryRepository,
		MediaReadService mediaReadService,
		SpecialistOptionRepository specialistOptionRepository,
		ObjectMapper objectMapper
	) {
		this.operationHistoryRepository = operationHistoryRepository;
		this.mediaReadService = mediaReadService;
		this.specialistOptionRepository = specialistOptionRepository;
		this.objectMapper = objectMapper;
	}

	@Transactional(propagation = Propagation.MANDATORY)
	public void record(
		AuthenticatedActor actor,
		Specialist specialist,
		String action,
		String reason,
		Map<String, String> before,
		Map<String, String> after
	) {
		OperationHistory history = new OperationHistory(
			OperationHistory.TARGET_SPECIALIST,
			specialist.id(),
			actor.actorType().name(),
			actor.accountId(),
			action,
			reason,
			null
		).captureActor(actor.name(), actor.loginId());
		Set<String> keys = new LinkedHashSet<>();
		keys.addAll(before.keySet());
		keys.addAll(after.keySet());
		for (String key : keys) {
			if (!Objects.equals(before.get(key), after.get(key))) {
				history.addChange(key, before.get(key), after.get(key));
			}
		}
		if (!history.changes().isEmpty() || !"UPDATED".equals(action)) {
			operationHistoryRepository.save(history);
		}
	}

	public Map<String, String> capture(Specialist specialist) {
		Map<String, String> values = new LinkedHashMap<>();
		values.put("partner_id", String.valueOf(specialist.partnerId()));
		values.put("sort_order", String.valueOf(specialist.sortOrder()));
		values.put("name", specialist.name());
		values.put("gender", specialist.gender());
		values.put("position", specialist.position());
		values.put("career_started_at", specialist.careerStartedAt() == null ? null : specialist.careerStartedAt().toString());
		values.put("specialist_field", specialist.specialistField().name());
		values.put("introduction", specialist.introduction());
		values.put("schedule_mode", specialist.scheduleMode().name());
		values.put("operation_hours", normalizeNullableJson(specialist.operationHours()));
		values.put("holiday_policy", normalizeNullableJson(specialist.holidayPolicy()));
		values.put("option_assignments", optionAssignments(specialist.id()));
		values.put("profile_images", media(specialist.id(), MediaCollectionPolicy.SPECIALIST_PROFILE_IMAGE));
		values.put("certification_images", media(specialist.id(), MediaCollectionPolicy.SPECIALIST_CERTIFICATION_IMAGE));
		values.put("status", specialist.status().name());
		values.put("allow_status", specialist.allowStatus().name());
		values.put(
			"reviewer_staff_id",
			specialist.reviewerStaff() == null ? null : String.valueOf(specialist.reviewerStaff().id())
		);
		values.put(
			"review_started_at",
			specialist.reviewStartedAt() == null ? null : specialist.reviewStartedAt().toString()
		);
		return values;
	}

	private String media(Long specialistId, String collection) {
		return writeJson(mediaReadService.list(MediaOwnerType.SPECIALIST, specialistId, collection).stream()
			.map(this::mediaValue)
			.toList());
	}

	private Map<String, Object> mediaValue(MediaResult media) {
		Map<String, Object> value = new LinkedHashMap<>();
		value.put("id", media.id());
		value.put("name", media.originalName());
		value.put("mime_type", media.mimeType());
		value.put("size", media.size());
		return value;
	}

	private String normalizeJson(String value) {
		if (value == null) {
			return "[]";
		}
		try {
			return objectMapper.writeValueAsString(objectMapper.readTree(value));
		} catch (JsonProcessingException exception) {
			throw new InternalApplicationException("저장된 스페셜리스트 이력 대상 JSON이 올바르지 않습니다.", exception);
		}
	}

	private String normalizeNullableJson(String value) {
		if (value == null) {
			return null;
		}
		return normalizeJson(value);
	}

	private String optionAssignments(Long specialistId) {
		List<Map<String, Object>> values = specialistOptionRepository
			.findBySpecialist_IdOrderByPartnerOption_SortOrderAscPartnerOption_IdAsc(specialistId)
			.stream()
			.map(assignment -> {
				Map<String, Object> value = new LinkedHashMap<>();
				value.put("partner_option_id", assignment.partnerOption().id());
				value.put("regular_price_override", assignment.regularPriceOverride());
				value.put("sale_price_override", assignment.salePriceOverride());
				return value;
			})
			.toList();
		return writeJson(values);
	}

	private String writeJson(Object value) {
		try {
			return objectMapper.writeValueAsString(value);
		} catch (JsonProcessingException exception) {
			throw new InternalApplicationException("스페셜리스트 변경 이력 JSON을 만들 수 없습니다.", exception);
		}
	}
}
