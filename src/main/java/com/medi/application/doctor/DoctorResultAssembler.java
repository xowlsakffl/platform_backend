package com.medi.application.doctor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.medi.application.doctor.result.DoctorCategoryResult;
import com.medi.application.doctor.result.DoctorDetailResult;
import com.medi.application.doctor.result.DoctorListItemResult;
import com.medi.application.doctor.result.DoctorListCategoryResult;
import com.medi.application.doctor.result.DoctorMediaResult;
import com.medi.application.doctor.result.DoctorSpecialistResult;
import com.medi.application.media.MediaCollectionPolicy;
import com.medi.application.media.MediaReadService;
import com.medi.application.media.result.MediaResult;
import com.medi.common.error.InternalApplicationException;
import com.medi.domain.category.CategoryAssignment;
import com.medi.domain.doctor.Doctor;
import com.medi.domain.doctor.DoctorSpecialistField;
import com.medi.domain.media.MediaOwnerType;
import com.medi.infrastructure.persistence.category.CategoryAssignmentRepository;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class DoctorResultAssembler {

	private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {
	};

	private final CategoryAssignmentRepository categoryAssignmentRepository;
	private final MediaReadService mediaReadService;
	private final ObjectMapper objectMapper;

	public DoctorResultAssembler(
		CategoryAssignmentRepository categoryAssignmentRepository,
		MediaReadService mediaReadService,
		ObjectMapper objectMapper
	) {
		this.categoryAssignmentRepository = categoryAssignmentRepository;
		this.mediaReadService = mediaReadService;
		this.objectMapper = objectMapper;
	}

	public DoctorDetailResult detail(Doctor doctor, DoctorMediaAccessScope scope) {
		List<DoctorCategoryResult> categories = categoriesByDoctorIds(List.of(doctor))
			.getOrDefault(doctor.id(), List.of());
		return new DoctorDetailResult(
			doctor.id(),
			doctor.hospitalId(),
			doctor.hospital().name(),
			doctor.hospital().businessRegistration() == null
				? null
				: doctor.hospital().businessRegistration().businessNumber(),
			doctor.sortOrder(),
			doctor.name(),
			doctor.gender(),
			doctor.position(),
			doctor.careerStartedAt(),
			doctor.licenseNumber(),
			specialist(doctor.specialistField()),
			doctor.status().name(),
			doctor.status().label(),
			doctor.allowStatus().name(),
			doctor.allowStatus().label(),
			fromJsonList(doctor.educations()),
			fromJsonList(doctor.careers()),
			fromJsonList(doctor.etcContents()),
			categories,
			media(mediaReadService.primary(
				MediaOwnerType.DOCTOR,
				doctor.id(),
				MediaCollectionPolicy.DOCTOR_PROFILE_IMAGE
			), doctor.id(), scope),
			media(mediaReadService.primary(
				MediaOwnerType.DOCTOR,
				doctor.id(),
				MediaCollectionPolicy.DOCTOR_LICENSE_IMAGE
			), doctor.id(), scope),
			media(mediaReadService.primary(
				MediaOwnerType.DOCTOR,
				doctor.id(),
				MediaCollectionPolicy.DOCTOR_SPECIALIST_CERTIFICATE_IMAGE
			), doctor.id(), scope),
			doctor.viewCount(),
			doctor.createdAt(),
			doctor.updatedAt()
		);
	}

	public DoctorListItemResult listItem(
		Doctor doctor,
		List<DoctorListCategoryResult> categories,
		MediaResult profileImage,
		DoctorMediaAccessScope scope
	) {
		return new DoctorListItemResult(
			doctor.id(),
			doctor.hospitalId(),
			doctor.hospital().name(),
			doctor.name(),
			doctor.gender(),
			doctor.position(),
			specialist(doctor.specialistField()),
			doctor.careerStartedAt(),
			doctor.licenseNumber(),
			doctor.allowStatus().name(),
			doctor.status().name(),
			0,
			0,
			doctor.createdAt(),
			media(profileImage, doctor.id(), scope),
			categories
		);
	}

	public Map<Long, List<DoctorListCategoryResult>> listCategoriesByDoctorIds(List<Doctor> doctors) {
		Map<Long, List<DoctorCategoryResult>> assigned = categoriesByDoctorIds(doctors);
		Map<Long, List<DoctorListCategoryResult>> result = new LinkedHashMap<>();
		for (Map.Entry<Long, List<DoctorCategoryResult>> entry : assigned.entrySet()) {
			Set<String> rootNames = new LinkedHashSet<>();
			for (DoctorCategoryResult category : entry.getValue()) {
				String fullPath = category.fullPath() == null ? category.name() : category.fullPath();
				String rootName = fullPath.split("\\s*>\\s*", 2)[0].trim();
				if (!rootName.isEmpty()) {
					rootNames.add(rootName);
				}
			}
			result.put(entry.getKey(), rootNames.stream().map(DoctorListCategoryResult::new).toList());
		}
		return result;
	}

	public Map<Long, List<DoctorCategoryResult>> categoriesByDoctorIds(List<Doctor> doctors) {
		Set<Long> ids = doctorIds(doctors);
		if (ids.isEmpty()) {
			return Map.of();
		}
		return categoryAssignmentRepository
			.findByCategorizableTypeAndCategorizableIdIn(CategoryAssignment.DOCTOR_TARGET_TYPE, ids)
			.stream()
			.sorted(Comparator.comparing((CategoryAssignment assignment) -> !assignment.primary())
				.thenComparing(assignment -> assignment.category().sortOrder())
				.thenComparing(assignment -> assignment.category().id()))
			.collect(Collectors.groupingBy(
				CategoryAssignment::categorizableId,
				LinkedHashMap::new,
				Collectors.mapping(assignment -> new DoctorCategoryResult(
					assignment.category().id(),
					assignment.category().domain().name(),
					assignment.category().name(),
					assignment.category().fullPath(),
					assignment.primary()
				), Collectors.toList())
			));
	}

	public Map<Long, MediaResult> profileImages(List<Doctor> doctors) {
		return mediaReadService.primaries(
			MediaOwnerType.DOCTOR,
			doctorIds(doctors),
			MediaCollectionPolicy.DOCTOR_PROFILE_IMAGE
		);
	}

	private Set<Long> doctorIds(List<Doctor> doctors) {
		return doctors.stream().map(Doctor::id).collect(Collectors.toCollection(LinkedHashSet::new));
	}

	private DoctorMediaResult media(MediaResult media, Long doctorId, DoctorMediaAccessScope scope) {
		if (media == null) {
			return null;
		}
		String url = switch (scope) {
			case STAFF -> media.contentUrl();
			case HOSPITAL -> "/api/v1/hospital/doctors/%d/media/%d/content".formatted(doctorId, media.id());
		};
		return new DoctorMediaResult(
			media.id(),
			url,
			media.mimeType(),
			media.size(),
			media.width(),
			media.height(),
			media.metadata()
		);
	}

	private DoctorSpecialistResult specialist(DoctorSpecialistField specialist) {
		return new DoctorSpecialistResult(specialist.code(), specialist.name(), specialist.label());
	}

	private List<String> fromJsonList(String value) {
		if (value == null) {
			return List.of();
		}
		try {
			return objectMapper.readValue(value, STRING_LIST_TYPE);
		} catch (JsonProcessingException exception) {
			throw new InternalApplicationException("저장된 의료진 목록 JSON이 올바르지 않습니다.", exception);
		}
	}
}
