package com.medi.application.media;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.medi.application.media.command.UpdateMediaCommand;
import com.medi.application.media.command.UploadMediaCommand;
import com.medi.application.media.query.SearchMediaQuery;
import com.medi.application.media.result.MediaContentResult;
import com.medi.application.media.result.MediaDeletedResult;
import com.medi.application.media.result.MediaResult;
import com.medi.application.media.storage.MediaContent;
import com.medi.application.media.storage.MediaStorage;
import com.medi.application.media.storage.MediaStorageException;
import com.medi.common.error.ApiException;
import com.medi.common.error.ErrorCode;
import com.medi.common.security.AuthenticatedActor;
import com.medi.domain.media.Media;
import com.medi.infrastructure.persistence.media.MediaRepository;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class MediaStaffService {

	private static final Logger log = LoggerFactory.getLogger(MediaStaffService.class);
	private static final Pattern COLLECTION_PATTERN = Pattern.compile("^[a-z][a-z0-9_]{0,49}$");
	private static final int MAX_SORT_ORDER = 100_000;
	private static final int MAX_METADATA_LENGTH = 10_000;
	private static final TypeReference<Map<String, Object>> METADATA_TYPE = new TypeReference<>() {
	};

	private final MediaOwnerPolicy ownerPolicy;
	private final MediaCollectionPolicy collectionPolicy;
	private final MediaCommandService commandService;
	private final MediaRepository mediaRepository;
	private final MediaStorage mediaStorage;
	private final ObjectMapper objectMapper;

	public MediaStaffService(
		MediaOwnerPolicy ownerPolicy,
		MediaCollectionPolicy collectionPolicy,
		MediaCommandService commandService,
		MediaRepository mediaRepository,
		MediaStorage mediaStorage,
		ObjectMapper objectMapper
	) {
		this.ownerPolicy = ownerPolicy;
		this.collectionPolicy = collectionPolicy;
		this.commandService = commandService;
		this.mediaRepository = mediaRepository;
		this.mediaStorage = mediaStorage;
		this.objectMapper = objectMapper;
	}

	@Transactional(readOnly = true)
	public List<MediaResult> list(AuthenticatedActor actor, SearchMediaQuery query) {
		String collection = normalizeCollection(query.collection());
		collectionPolicy.validateCollection(query.ownerType(), collection);
		ownerPolicy.requireReadable(actor, query.ownerType(), query.ownerId());

		return mediaRepository
			.findByOwnerTypeAndOwnerIdAndCollectionAndDeletedAtIsNullOrderBySortOrderAscIdAsc(
				query.ownerType(),
				query.ownerId(),
				collection
			)
			.stream()
			.map(this::toResult)
			.toList();
	}

	@Transactional(readOnly = true)
	public MediaResult get(AuthenticatedActor actor, Long id) {
		Media media = findActiveMedia(id);
		ownerPolicy.requireReadable(actor, media.ownerType(), media.ownerId());
		return toResult(media);
	}

	@Transactional(readOnly = true)
	public MediaContentResult content(AuthenticatedActor actor, Long id) {
		Media media = findActiveMedia(id);
		ownerPolicy.requireReadable(actor, media.ownerType(), media.ownerId());
		MediaContent content = loadContent(media.path());
		return new MediaContentResult(media.originalName(), media.mimeType(), content.contentLength(), content);
	}

	@Transactional
	public MediaResult upload(AuthenticatedActor actor, UploadMediaCommand command) {
		String collection = normalizeCollection(command.collection());
		collectionPolicy.validateCollection(command.ownerType(), collection);
		ownerPolicy.requireMutable(actor, command.ownerType(), command.ownerId());
		return commandService.upload(new UploadMediaCommand(
			command.ownerType(),
			command.ownerId(),
			collection,
			command.sortOrder(),
			command.primary(),
			command.metadata(),
			command.file()
		));
	}

	@Transactional
	public MediaResult update(AuthenticatedActor actor, Long id, UpdateMediaCommand command) {
		if (command.sortOrder() == null && command.primary() == null && !command.metadataSpecified()) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "변경할 미디어 정보가 없습니다.");
		}
		Media media = findMutableMedia(actor, id);

		if (command.sortOrder() != null) {
			media.changeSortOrder(validateSortOrder(command.sortOrder()));
		}
		if (Boolean.TRUE.equals(command.primary())) {
			mediaRepository
				.findLockedByOwnerTypeAndOwnerIdAndCollectionAndDeletedAtIsNullOrderBySortOrderAscIdAsc(
					media.ownerType(),
					media.ownerId(),
					media.collection()
				)
				.forEach(item -> item.changePrimary(item.id().equals(media.id())));
		} else if (Boolean.FALSE.equals(command.primary())) {
			media.changePrimary(false);
		}
		if (command.metadataSpecified()) {
			media.changeMetadata(normalizeMetadata(command.metadata()));
		}

		return toResult(mediaRepository.saveAndFlush(media));
	}

	@Transactional
	public MediaDeletedResult delete(AuthenticatedActor actor, Long id) {
		Media media = findMutableMedia(actor, id);
		boolean wasPrimary = media.primary();
		media.softDelete();

		if (wasPrimary) {
			mediaRepository
				.findLockedByOwnerTypeAndOwnerIdAndCollectionAndDeletedAtIsNullOrderBySortOrderAscIdAsc(
					media.ownerType(),
					media.ownerId(),
					media.collection()
				)
				.stream()
				.filter(item -> !item.id().equals(media.id()))
				.findFirst()
				.ifPresent(item -> item.changePrimary(true));
		}
		mediaRepository.saveAndFlush(media);
		return new MediaDeletedResult(media.id(), media.deletedAt());
	}

	private Media findActiveMedia(Long id) {
		return mediaRepository.findByIdAndDeletedAtIsNull(id)
			.orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "미디어를 찾을 수 없습니다."));
	}

	private Media findLockedActiveMedia(Long id) {
		return mediaRepository.findLockedByIdAndDeletedAtIsNull(id)
			.orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "미디어를 찾을 수 없습니다."));
	}

	private Media findMutableMedia(AuthenticatedActor actor, Long id) {
		Media reference = findActiveMedia(id);
		ownerPolicy.requireMutable(actor, reference.ownerType(), reference.ownerId());
		return findLockedActiveMedia(id);
	}

	private MediaContent loadContent(String path) {
		try {
			return mediaStorage.load(path);
		} catch (MediaStorageException exception) {
			throw toApiException(exception);
		}
	}

	private ApiException toApiException(MediaStorageException exception) {
		return switch (exception.reason()) {
			case INVALID_FILE -> new ApiException(ErrorCode.INVALID_REQUEST, exception.getMessage());
			case FILE_TOO_LARGE -> new ApiException(ErrorCode.PAYLOAD_TOO_LARGE, exception.getMessage());
			case FILE_NOT_FOUND -> new ApiException(ErrorCode.NOT_FOUND, exception.getMessage());
			case IO_ERROR -> {
				log.error("미디어 저장소 처리 중 오류가 발생했습니다.", exception);
				yield new ApiException(ErrorCode.INTERNAL_ERROR, "미디어 파일 처리 중 오류가 발생했습니다.");
			}
		};
	}

	private String normalizeCollection(String collection) {
		String normalized = StringUtils.hasText(collection) ? collection.trim() : null;
		if (normalized == null || !COLLECTION_PATTERN.matcher(normalized).matches()) {
			throw new ApiException(
				ErrorCode.INVALID_REQUEST,
				"컬렉션은 영문 소문자로 시작하고 영문 소문자, 숫자, 밑줄만 사용할 수 있습니다."
			);
		}
		return normalized;
	}

	private int validateSortOrder(int sortOrder) {
		if (sortOrder < 0 || sortOrder > MAX_SORT_ORDER) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "정렬 순서는 0 이상 100000 이하여야 합니다.");
		}
		return sortOrder;
	}

	private String normalizeMetadata(String metadata) {
		if (!StringUtils.hasText(metadata)) {
			return null;
		}
		if (metadata.length() > MAX_METADATA_LENGTH) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "미디어 메타데이터는 10000자를 초과할 수 없습니다.");
		}
		try {
			Map<String, Object> value = objectMapper.readValue(metadata, METADATA_TYPE);
			if (value == null) {
				throw new ApiException(ErrorCode.INVALID_REQUEST, "미디어 메타데이터는 JSON 객체여야 합니다.");
			}
			return objectMapper.writeValueAsString(value);
		} catch (JsonProcessingException exception) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "미디어 메타데이터 JSON이 올바르지 않습니다.");
		}
	}

	private String normalizeMetadata(Map<String, Object> metadata) {
		if (metadata == null) {
			return null;
		}
		try {
			String normalized = objectMapper.writeValueAsString(metadata);
			if (normalized.length() > MAX_METADATA_LENGTH) {
				throw new ApiException(ErrorCode.INVALID_REQUEST, "미디어 메타데이터는 10000자를 초과할 수 없습니다.");
			}
			return normalized;
		} catch (JsonProcessingException exception) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "미디어 메타데이터 JSON이 올바르지 않습니다.");
		}
	}

	private Map<String, Object> parseMetadata(String metadata) {
		if (metadata == null) {
			return null;
		}
		try {
			return objectMapper.readValue(metadata, METADATA_TYPE);
		} catch (JsonProcessingException exception) {
			throw new IllegalStateException("저장된 미디어 메타데이터 JSON이 올바르지 않습니다.", exception);
		}
	}

	private MediaResult toResult(Media media) {
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

}
