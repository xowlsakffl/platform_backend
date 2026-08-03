package com.platform.application.specialist;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.application.specialist.result.SpecialistDetailResult;
import com.platform.application.specialist.result.SpecialistListItemResult;
import com.platform.application.specialist.result.SpecialistMediaResult;
import com.platform.application.specialist.result.SpecialistFieldResult;
import com.platform.application.media.MediaCollectionPolicy;
import com.platform.application.media.MediaReadService;
import com.platform.application.media.result.MediaResult;
import com.platform.common.error.InternalApplicationException;
import com.platform.domain.specialist.Specialist;
import com.platform.domain.specialist.SpecialistField;
import com.platform.domain.media.MediaOwnerType;
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

	private final MediaReadService mediaReadService;
	private final ObjectMapper objectMapper;

	public SpecialistResultAssembler(
		MediaReadService mediaReadService,
		ObjectMapper objectMapper
	) {
		this.mediaReadService = mediaReadService;
		this.objectMapper = objectMapper;
	}

	public SpecialistDetailResult detail(Specialist specialist, SpecialistMediaAccessScope scope) {
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
			media(profileImage, specialist.id(), scope)
		);
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
