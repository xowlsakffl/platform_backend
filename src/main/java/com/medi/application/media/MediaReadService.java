package com.medi.application.media;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.medi.application.media.result.MediaResult;
import com.medi.common.error.InternalApplicationException;
import com.medi.domain.media.Media;
import com.medi.domain.media.MediaOwnerType;
import com.medi.infrastructure.persistence.media.MediaRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MediaReadService {

	private static final TypeReference<Map<String, Object>> METADATA_TYPE = new TypeReference<>() {
	};

	private final MediaRepository mediaRepository;
	private final ObjectMapper objectMapper;

	public MediaReadService(MediaRepository mediaRepository, ObjectMapper objectMapper) {
		this.mediaRepository = mediaRepository;
		this.objectMapper = objectMapper;
	}

	@Transactional(readOnly = true)
	public List<MediaResult> list(MediaOwnerType ownerType, Long ownerId, String collection) {
		return mediaRepository
			.findByOwnerTypeAndOwnerIdAndCollectionAndDeletedAtIsNullOrderBySortOrderAscIdAsc(
				ownerType,
				ownerId,
				collection
			)
			.stream()
			.map(this::toResult)
			.toList();
	}

	@Transactional(readOnly = true)
	public MediaResult primary(MediaOwnerType ownerType, Long ownerId, String collection) {
		return mediaRepository
			.findByOwnerTypeAndOwnerIdAndCollectionAndDeletedAtIsNullOrderBySortOrderAscIdAsc(
				ownerType,
				ownerId,
				collection
			)
			.stream()
			.sorted((left, right) -> Boolean.compare(right.primary(), left.primary()))
			.findFirst()
			.map(this::toResult)
			.orElse(null);
	}

	@Transactional(readOnly = true)
	public Map<Long, MediaResult> primaries(MediaOwnerType ownerType, Set<Long> ownerIds, String collection) {
		if (ownerIds == null || ownerIds.isEmpty()) {
			return Map.of();
		}
		List<Media> media = mediaRepository
			.findByOwnerTypeAndOwnerIdInAndCollectionAndDeletedAtIsNullOrderByOwnerIdAscSortOrderAscIdAsc(
				ownerType,
				ownerIds,
				collection
			);
		Map<Long, MediaResult> result = new LinkedHashMap<>();
		for (Media item : media) {
			MediaResult existing = result.get(item.ownerId());
			if (existing == null || item.primary()) {
				result.put(item.ownerId(), toResult(item));
			}
		}
		return result;
	}

	public MediaResult toResult(Media media) {
		return new MediaResult(
			media.id(),
			media.ownerType(),
			media.ownerId(),
			media.collection(),
			media.disk(),
			media.originalName(),
			media.mimeType(),
			media.size(),
			media.width(),
			media.height(),
			media.sortOrder(),
			media.primary(),
			parseMetadata(media.metadata()),
			"/api/v1/staff/media/%d/content".formatted(media.id()),
			media.createdAt(),
			media.updatedAt()
		);
	}

	private Map<String, Object> parseMetadata(String metadata) {
		if (metadata == null) {
			return null;
		}
		try {
			return objectMapper.readValue(metadata, METADATA_TYPE);
		} catch (JsonProcessingException exception) {
			throw new InternalApplicationException("저장된 미디어 메타데이터 JSON이 올바르지 않습니다.", exception);
		}
	}
}
