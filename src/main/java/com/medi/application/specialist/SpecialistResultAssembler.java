package com.medi.application.specialist;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.medi.application.specialist.result.SpecialistCategoryResult;
import com.medi.application.specialist.result.SpecialistDetailResult;
import com.medi.application.specialist.result.SpecialistListItemResult;
import com.medi.application.specialist.result.SpecialistListCategoryResult;
import com.medi.application.specialist.result.SpecialistMediaResult;
import com.medi.application.specialist.result.SpecialistFieldResult;
import com.medi.application.media.MediaCollectionPolicy;
import com.medi.application.media.MediaReadService;
import com.medi.application.media.result.MediaResult;
import com.medi.common.error.InternalApplicationException;
import com.medi.domain.category.CategoryAssignment;
import com.medi.domain.specialist.Specialist;
import com.medi.domain.specialist.SpecialistField;
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
public class SpecialistResultAssembler {

	private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {
	};

	private final CategoryAssignmentRepository categoryAssignmentRepository;
	private final MediaReadService mediaReadService;
	private final ObjectMapper objectMapper;

	public SpecialistResultAssembler(
		CategoryAssignmentRepository categoryAssignmentRepository,
		MediaReadService mediaReadService,
		ObjectMapper objectMapper
	) {
		this.categoryAssignmentRepository = categoryAssignmentRepository;
		this.mediaReadService = mediaReadService;
		this.objectMapper = objectMapper;
	}

	public SpecialistDetailResult detail(Specialist specialist, SpecialistMediaAccessScope scope) {
		List<SpecialistCategoryResult> categories = categoriesBySpecialistIds(List.of(specialist))
			.getOrDefault(specialist.id(), List.of());
		return new SpecialistDetailResult(
			specialist.id(),
			specialist.partnerId(),
			specialist.partner().name(),
			specialist.partner().businessRegistration() == null
				? null
				: specialist.partner().businessRegistration().businessNumber(),
			specialist.sortOrder(),
			specialist.name(),
			specialist.gender(),
			specialist.position(),
			specialist.careerStartedAt(),
			specialist.licenseNumber(),
			specialist(specialist.specialistField()),
			specialist.status().name(),
			specialist.status().label(),
			specialist.allowStatus().name(),
			specialist.allowStatus().label(),
			fromJsonList(specialist.educations()),
			fromJsonList(specialist.careers()),
			fromJsonList(specialist.etcContents()),
			categories,
			media(mediaReadService.primary(
				MediaOwnerType.SPECIALIST,
				specialist.id(),
				MediaCollectionPolicy.SPECIALIST_PROFILE_IMAGE
			), specialist.id(), scope),
			media(mediaReadService.primary(
				MediaOwnerType.SPECIALIST,
				specialist.id(),
				MediaCollectionPolicy.SPECIALIST_LICENSE_IMAGE
			), specialist.id(), scope),
			media(mediaReadService.primary(
				MediaOwnerType.SPECIALIST,
				specialist.id(),
				MediaCollectionPolicy.SPECIALIST_SPECIALIST_CERTIFICATE_IMAGE
			), specialist.id(), scope),
			specialist.viewCount(),
			specialist.createdAt(),
			specialist.updatedAt()
		);
	}

	public SpecialistListItemResult listItem(
		Specialist specialist,
		List<SpecialistListCategoryResult> categories,
		MediaResult profileImage,
		SpecialistMediaAccessScope scope
	) {
		return new SpecialistListItemResult(
			specialist.id(),
			specialist.partnerId(),
			specialist.partner().name(),
			specialist.name(),
			specialist.gender(),
			specialist.position(),
			specialist(specialist.specialistField()),
			specialist.careerStartedAt(),
			specialist.licenseNumber(),
			specialist.allowStatus().name(),
			specialist.status().name(),
			0,
			0,
			specialist.createdAt(),
			media(profileImage, specialist.id(), scope),
			categories
		);
	}

	public Map<Long, List<SpecialistListCategoryResult>> listCategoriesBySpecialistIds(List<Specialist> specialists) {
		Map<Long, List<SpecialistCategoryResult>> assigned = categoriesBySpecialistIds(specialists);
		Map<Long, List<SpecialistListCategoryResult>> result = new LinkedHashMap<>();
		for (Map.Entry<Long, List<SpecialistCategoryResult>> entry : assigned.entrySet()) {
			Set<String> rootNames = new LinkedHashSet<>();
			for (SpecialistCategoryResult category : entry.getValue()) {
				String fullPath = category.fullPath() == null ? category.name() : category.fullPath();
				String rootName = fullPath.split("\\s*>\\s*", 2)[0].trim();
				if (!rootName.isEmpty()) {
					rootNames.add(rootName);
				}
			}
			result.put(entry.getKey(), rootNames.stream().map(SpecialistListCategoryResult::new).toList());
		}
		return result;
	}

	public Map<Long, List<SpecialistCategoryResult>> categoriesBySpecialistIds(List<Specialist> specialists) {
		Set<Long> ids = specialistIds(specialists);
		if (ids.isEmpty()) {
			return Map.of();
		}
		return categoryAssignmentRepository
			.findByCategorizableTypeAndCategorizableIdIn(CategoryAssignment.SPECIALIST_TARGET_TYPE, ids)
			.stream()
			.sorted(Comparator.comparing((CategoryAssignment assignment) -> !assignment.primary())
				.thenComparing(assignment -> assignment.category().sortOrder())
				.thenComparing(assignment -> assignment.category().id()))
			.collect(Collectors.groupingBy(
				CategoryAssignment::categorizableId,
				LinkedHashMap::new,
				Collectors.mapping(assignment -> new SpecialistCategoryResult(
					assignment.category().id(),
					assignment.category().domain().name(),
					assignment.category().name(),
					assignment.category().fullPath(),
					assignment.primary()
				), Collectors.toList())
			));
	}

	public Map<Long, MediaResult> profileImages(List<Specialist> specialists) {
		return mediaReadService.primaries(
			MediaOwnerType.SPECIALIST,
			specialistIds(specialists),
			MediaCollectionPolicy.SPECIALIST_PROFILE_IMAGE
		);
	}

	private Set<Long> specialistIds(List<Specialist> specialists) {
		return specialists.stream().map(Specialist::id).collect(Collectors.toCollection(LinkedHashSet::new));
	}

	private SpecialistMediaResult media(MediaResult media, Long specialistId, SpecialistMediaAccessScope scope) {
		if (media == null) {
			return null;
		}
		String url = switch (scope) {
			case STAFF -> media.contentUrl();
			case PARTNER -> "/api/v1/partner/specialists/%d/media/%d/content".formatted(specialistId, media.id());
		};
		return new SpecialistMediaResult(
			media.id(),
			url,
			media.mimeType(),
			media.size(),
			media.width(),
			media.height(),
			media.metadata()
		);
	}

	private SpecialistFieldResult specialist(SpecialistField specialist) {
		return new SpecialistFieldResult(specialist.code(), specialist.name(), specialist.label());
	}

	private List<String> fromJsonList(String value) {
		if (value == null) {
			return List.of();
		}
		try {
			return objectMapper.readValue(value, STRING_LIST_TYPE);
		} catch (JsonProcessingException exception) {
			throw new InternalApplicationException("저장된 스페셜리스트 목록 JSON이 올바르지 않습니다.", exception);
		}
	}
}
