package com.medi.application.doctor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.medi.application.media.MediaCollectionPolicy;
import com.medi.application.media.MediaReadService;
import com.medi.application.media.result.MediaResult;
import com.medi.common.error.InternalApplicationException;
import com.medi.common.security.AuthenticatedActor;
import com.medi.domain.category.CategoryAssignment;
import com.medi.domain.doctor.Doctor;
import com.medi.domain.media.MediaOwnerType;
import com.medi.domain.operationhistory.OperationHistory;
import com.medi.infrastructure.persistence.category.CategoryAssignmentRepository;
import com.medi.infrastructure.persistence.operationhistory.OperationHistoryRepository;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DoctorHistoryService {

	private final OperationHistoryRepository operationHistoryRepository;
	private final CategoryAssignmentRepository categoryAssignmentRepository;
	private final MediaReadService mediaReadService;
	private final ObjectMapper objectMapper;

	public DoctorHistoryService(
		OperationHistoryRepository operationHistoryRepository,
		CategoryAssignmentRepository categoryAssignmentRepository,
		MediaReadService mediaReadService,
		ObjectMapper objectMapper
	) {
		this.operationHistoryRepository = operationHistoryRepository;
		this.categoryAssignmentRepository = categoryAssignmentRepository;
		this.mediaReadService = mediaReadService;
		this.objectMapper = objectMapper;
	}

	@Transactional(propagation = Propagation.MANDATORY)
	public void record(
		AuthenticatedActor actor,
		Doctor doctor,
		String action,
		String reason,
		Map<String, String> before,
		Map<String, String> after
	) {
		OperationHistory history = new OperationHistory(
			OperationHistory.TARGET_DOCTOR,
			doctor.id(),
			actor.actorType().name(),
			actor.accountId(),
			action,
			reason,
			null
		);
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

	public Map<String, String> capture(Doctor doctor) {
		Map<String, String> values = new LinkedHashMap<>();
		values.put("hospital_id", String.valueOf(doctor.hospitalId()));
		values.put("sort_order", String.valueOf(doctor.sortOrder()));
		values.put("name", doctor.name());
		values.put("gender", doctor.gender());
		values.put("position", doctor.position());
		values.put("career_started_at", doctor.careerStartedAt() == null ? null : doctor.careerStartedAt().toString());
		values.put("license_number", doctor.licenseNumber());
		values.put("specialist_field", doctor.specialistField().name());
		values.put("categories", categories(doctor.id()));
		values.put("educations", normalizeJson(doctor.educations()));
		values.put("careers", normalizeJson(doctor.careers()));
		values.put("etc_contents", normalizeJson(doctor.etcContents()));
		values.put("profile_image", media(doctor.id(), MediaCollectionPolicy.DOCTOR_PROFILE_IMAGE));
		values.put("license_image", media(doctor.id(), MediaCollectionPolicy.DOCTOR_LICENSE_IMAGE));
		values.put(
			"specialist_certificate_image",
			media(doctor.id(), MediaCollectionPolicy.DOCTOR_SPECIALIST_CERTIFICATE_IMAGE)
		);
		values.put("status", doctor.status().name());
		values.put("allow_status", doctor.allowStatus().name());
		return values;
	}

	private String categories(Long doctorId) {
		List<Map<String, Object>> items = categoryAssignmentRepository
			.findByCategorizableTypeAndCategorizableId(CategoryAssignment.DOCTOR_TARGET_TYPE, doctorId)
			.stream()
			.sorted(Comparator.comparing((CategoryAssignment assignment) -> !assignment.primary())
				.thenComparing(assignment -> Objects.requireNonNullElse(
					assignment.category().fullPath(),
					assignment.category().name()
				))
				.thenComparing(assignment -> assignment.category().id()))
			.map(assignment -> {
				Map<String, Object> item = new LinkedHashMap<>();
				item.put("id", assignment.category().id());
				item.put("path", Objects.requireNonNullElse(
					assignment.category().fullPath(),
					assignment.category().name()
				));
				item.put("is_primary", assignment.primary());
				return item;
			})
			.toList();
		return writeJson(items);
	}

	private String media(Long doctorId, String collection) {
		MediaResult media = mediaReadService.primary(MediaOwnerType.DOCTOR, doctorId, collection);
		if (media == null) {
			return null;
		}
		Map<String, Object> value = new LinkedHashMap<>();
		value.put("id", media.id());
		value.put("name", media.originalName());
		value.put("mime_type", media.mimeType());
		value.put("size", media.size());
		return writeJson(value);
	}

	private String normalizeJson(String value) {
		if (value == null) {
			return "[]";
		}
		try {
			return objectMapper.writeValueAsString(objectMapper.readTree(value));
		} catch (JsonProcessingException exception) {
			throw new InternalApplicationException("저장된 의료진 이력 대상 JSON이 올바르지 않습니다.", exception);
		}
	}

	private String writeJson(Object value) {
		try {
			return objectMapper.writeValueAsString(value);
		} catch (JsonProcessingException exception) {
			throw new InternalApplicationException("의료진 변경 이력 JSON을 만들 수 없습니다.", exception);
		}
	}
}
