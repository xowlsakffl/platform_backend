package com.platform.application.specialist;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.application.specialist.result.SpecialistDetailResult;
import com.platform.application.specialist.result.SpecialistListItemResult;
import com.platform.application.specialist.result.SpecialistMediaResult;
import com.platform.application.specialist.result.SpecialistOptionResult;
import com.platform.application.specialist.result.SpecialistFieldResult;
import com.platform.application.media.MediaCollectionPolicy;
import com.platform.application.media.MediaReadService;
import com.platform.application.media.result.MediaResult;
import com.platform.common.error.InternalApplicationException;
import com.platform.domain.specialist.Specialist;
import com.platform.domain.specialist.SpecialistField;
import com.platform.domain.specialist.SpecialistOption;
import com.platform.domain.media.MediaOwnerType;
import com.platform.infrastructure.persistence.specialist.SpecialistOptionCount;
import com.platform.infrastructure.persistence.specialist.SpecialistOptionRepository;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class SpecialistResultAssembler {

	private final MediaReadService mediaReadService;
	private final SpecialistOptionRepository specialistOptionRepository;
	private final ObjectMapper objectMapper;

	public SpecialistResultAssembler(
		MediaReadService mediaReadService,
		SpecialistOptionRepository specialistOptionRepository,
		ObjectMapper objectMapper
	) {
		this.mediaReadService = mediaReadService;
		this.specialistOptionRepository = specialistOptionRepository;
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
			specialist.introduction(),
			specialist.careerStartedAt(),
			specialistField(specialist),
			specialist.status().name(),
			specialist.status().label(),
			specialist.allowStatus().name(),
			specialist.allowStatus().label(),
			specialist.scheduleMode().name(),
			specialist.scheduleMode().label(),
			fromJsonObject(specialist.operationHours()),
			fromJsonObject(specialist.holidayPolicy()),
			specialist.reviewerStaff() == null ? null : specialist.reviewerStaff().id(),
			specialist.reviewerStaff() == null ? null : specialist.reviewerStaff().name(),
			specialist.reviewStartedAt(),
			options(specialist.id()),
			media(mediaReadService.primary(
				MediaOwnerType.SPECIALIST,
				specialist.id(),
				MediaCollectionPolicy.SPECIALIST_PROFILE_IMAGE
			), specialist.id(), scope),
			mediaList(mediaReadService.list(
				MediaOwnerType.SPECIALIST,
				specialist.id(),
				MediaCollectionPolicy.SPECIALIST_PROFILE_IMAGE
			), specialist.id(), scope),
			mediaList(mediaReadService.list(
				MediaOwnerType.SPECIALIST,
				specialist.id(),
				MediaCollectionPolicy.SPECIALIST_CERTIFICATION_IMAGE
			), specialist.id(), scope),
			specialist.viewCount(),
			specialist.createdAt(),
			specialist.updatedAt()
		);
	}

	public SpecialistListItemResult listItem(
		Specialist specialist,
		MediaResult profileImage,
		long optionCount,
		SpecialistMediaAccessScope scope
	) {
		return new SpecialistListItemResult(
			specialist.id(),
			specialist.partnerId(),
			specialist.partner().name(),
			specialist.sortOrder(),
			specialist.name(),
			specialist.gender(),
			specialist.position(),
			specialist.introduction(),
			specialistField(specialist),
			specialist.careerStartedAt(),
			specialist.status().name(),
			specialist.status().label(),
			specialist.allowStatus().name(),
			specialist.allowStatus().label(),
			specialist.scheduleMode().name(),
			specialist.scheduleMode().label(),
			optionCount,
			media(profileImage, specialist.id(), scope),
			specialist.createdAt(),
			specialist.updatedAt()
		);
	}

	public Map<Long, MediaResult> profileImages(List<Specialist> specialists) {
		return mediaReadService.primaries(
			MediaOwnerType.SPECIALIST,
			specialistIds(specialists),
			MediaCollectionPolicy.SPECIALIST_PROFILE_IMAGE
		);
	}

	public Map<Long, Long> optionCounts(List<Specialist> specialists) {
		Set<Long> ids = specialistIds(specialists);
		if (ids.isEmpty()) {
			return Map.of();
		}
		return specialistOptionRepository.countBySpecialistIds(ids).stream().collect(Collectors.toMap(
			SpecialistOptionCount::getSpecialistId,
			SpecialistOptionCount::getItemCount,
			(first, second) -> first,
			LinkedHashMap::new
		));
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

	private List<SpecialistMediaResult> mediaList(
		List<MediaResult> media,
		Long specialistId,
		SpecialistMediaAccessScope scope
	) {
		return media.stream().map(item -> media(item, specialistId, scope)).toList();
	}

	private SpecialistFieldResult specialistField(Specialist specialist) {
		SpecialistField field = specialist.specialistField();
		return new SpecialistFieldResult(field.code(), field.name(), field.label());
	}

	private List<SpecialistOptionResult> options(Long specialistId) {
		return specialistOptionRepository
			.findBySpecialist_IdOrderByPartnerOption_SortOrderAscPartnerOption_IdAsc(specialistId)
			.stream()
			.map(this::option)
			.toList();
	}

	private SpecialistOptionResult option(SpecialistOption assignment) {
		var option = assignment.partnerOption();
		return new SpecialistOptionResult(
			assignment.id(),
			option.id(),
			option.name(),
			option.description(),
			option.regularPrice(),
			option.salePrice(),
			option.durationMinutes(),
			option.visible(),
			assignment.regularPriceOverride(),
			assignment.salePriceOverride(),
			assignment.effectiveRegularPrice(),
			assignment.effectiveSalePrice(),
			assignment.effectivePrice(),
			assignment.effectiveDiscountRate()
		);
	}

	private Object fromJsonObject(String value) {
		if (value == null) {
			return null;
		}
		try {
			return objectMapper.readValue(value, Object.class);
		} catch (JsonProcessingException exception) {
			throw new InternalApplicationException("저장된 전문가 운영정보 JSON이 올바르지 않습니다.", exception);
		}
	}
}
