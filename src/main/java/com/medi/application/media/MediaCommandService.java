package com.medi.application.media;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.medi.application.media.command.UploadMediaCommand;
import com.medi.application.media.result.MediaResult;
import com.medi.application.media.storage.MediaFileSource;
import com.medi.application.media.storage.MediaStorage;
import com.medi.application.media.storage.MediaStorageException;
import com.medi.application.media.storage.StoredMediaFile;
import com.medi.common.error.ApiException;
import com.medi.common.error.ErrorCode;
import com.medi.domain.media.Media;
import com.medi.domain.media.MediaOwnerType;
import com.medi.infrastructure.persistence.media.MediaRepository;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

@Service
public class MediaCommandService {

	private static final Logger log = LoggerFactory.getLogger(MediaCommandService.class);
	private static final int MAX_SORT_ORDER = 100_000;
	private static final int MAX_METADATA_LENGTH = 10_000;
	private static final TypeReference<Map<String, Object>> METADATA_TYPE = new TypeReference<>() {
	};

	private final MediaRepository mediaRepository;
	private final MediaStorage mediaStorage;
	private final MediaCollectionPolicy collectionPolicy;
	private final MediaReadService readService;
	private final ObjectMapper objectMapper;

	public MediaCommandService(
		MediaRepository mediaRepository,
		MediaStorage mediaStorage,
		MediaCollectionPolicy collectionPolicy,
		MediaReadService readService,
		ObjectMapper objectMapper
	) {
		this.mediaRepository = mediaRepository;
		this.mediaStorage = mediaStorage;
		this.collectionPolicy = collectionPolicy;
		this.readService = readService;
		this.objectMapper = objectMapper;
	}

	@Transactional(propagation = Propagation.MANDATORY)
	public MediaResult upload(UploadMediaCommand command) {
		collectionPolicy.validateCollection(command.ownerType(), command.collection());
		String metadata = normalizeMetadata(command.metadata());
		List<Media> collectionMedia = lockedCollection(command.ownerType(), command.ownerId(), command.collection());
		boolean primary = Boolean.TRUE.equals(command.primary()) || collectionMedia.isEmpty();
		if (primary) {
			collectionMedia.forEach(media -> media.changePrimary(false));
		}
		int sortOrder = command.sortOrder() == null ? nextSortOrder(collectionMedia) : validateSortOrder(command.sortOrder());
		StoredMediaFile stored = store(command.file());
		registerRollbackCleanup(stored.path());
		collectionPolicy.validateFile(command.ownerType(), command.collection(), stored);

		Media media = createMedia(
			command.ownerType(),
			command.ownerId(),
			command.collection(),
			stored,
			sortOrder,
			primary,
			metadata
		);
		return readService.toResult(mediaRepository.saveAndFlush(media));
	}

	@Transactional(propagation = Propagation.MANDATORY)
	public MediaResult synchronizeSingle(
		MediaOwnerType ownerType,
		Long ownerId,
		String collection,
		MediaFileSource newFile,
		Long existingMediaId,
		boolean required
	) {
		collectionPolicy.validateCollection(ownerType, collection);
		List<Media> current = lockedCollection(ownerType, ownerId, collection);
		if (newFile != null && newFile.size() > 0) {
			StoredMediaFile stored = store(newFile);
			registerRollbackCleanup(stored.path());
			collectionPolicy.validateFile(ownerType, collection, stored);
			current.forEach(Media::softDelete);
			Media media = createMedia(ownerType, ownerId, collection, stored, 0, true, null);
			return readService.toResult(mediaRepository.saveAndFlush(media));
		}

		if (existingMediaId != null) {
			Media selected = current.stream()
				.filter(media -> media.id().equals(existingMediaId))
				.findFirst()
				.orElseThrow(() -> new ApiException(ErrorCode.INVALID_REQUEST, "유지할 미디어 정보가 올바르지 않습니다."));
			for (Media media : current) {
				if (media.id().equals(selected.id())) {
					media.changePrimary(true);
					media.changeSortOrder(0);
				} else {
					media.softDelete();
				}
			}
			return readService.toResult(selected);
		}

		if (required) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "필수 미디어 파일을 등록해주세요.");
		}
		current.forEach(Media::softDelete);
		return null;
	}

	private List<Media> lockedCollection(MediaOwnerType ownerType, Long ownerId, String collection) {
		return mediaRepository.findLockedByOwnerTypeAndOwnerIdAndCollectionAndDeletedAtIsNullOrderBySortOrderAscIdAsc(
			ownerType,
			ownerId,
			collection
		);
	}

	private Media createMedia(
		MediaOwnerType ownerType,
		Long ownerId,
		String collection,
		StoredMediaFile stored,
		int sortOrder,
		boolean primary,
		String metadata
	) {
		return new Media(
			ownerType,
			ownerId,
			collection,
			stored.disk(),
			stored.path(),
			stored.originalName(),
			stored.mimeType(),
			stored.size(),
			stored.width(),
			stored.height(),
			sortOrder,
			primary,
			metadata
		);
	}

	private StoredMediaFile store(MediaFileSource source) {
		try {
			return mediaStorage.store(source);
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

	private int validateSortOrder(int sortOrder) {
		if (sortOrder < 0 || sortOrder > MAX_SORT_ORDER) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "정렬 순서는 0 이상 100000 이하여야 합니다.");
		}
		return sortOrder;
	}

	private int nextSortOrder(List<Media> media) {
		int currentMax = media.stream().mapToInt(Media::sortOrder).max().orElse(-1);
		return currentMax >= MAX_SORT_ORDER ? MAX_SORT_ORDER : currentMax + 1;
	}

	private void registerRollbackCleanup(String path) {
		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			return;
		}
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCompletion(int status) {
				if (status == TransactionSynchronization.STATUS_COMMITTED) {
					return;
				}
				try {
					mediaStorage.delete(path);
				} catch (RuntimeException exception) {
					log.warn("롤백된 미디어 파일 정리에 실패했습니다. path={}", path, exception);
				}
			}
		});
	}
}
