package com.platform.application.media;

import com.platform.application.media.result.MediaResult;
import com.platform.application.media.storage.MediaFileSource;
import com.platform.application.media.storage.MediaStorage;
import com.platform.application.media.storage.MediaStorageException;
import com.platform.application.media.storage.StoredMediaFile;
import com.platform.common.error.ApiException;
import com.platform.common.error.ErrorCode;
import com.platform.common.error.InternalApplicationException;
import com.platform.domain.media.Media;
import com.platform.domain.media.MediaOwnerType;
import com.platform.infrastructure.persistence.media.MediaRepository;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class MediaCommandService {

	private static final Logger log = LoggerFactory.getLogger(MediaCommandService.class);
	private final MediaRepository mediaRepository;
	private final MediaStorage mediaStorage;
	private final MediaCollectionPolicy collectionPolicy;
	private final MediaReadService readService;
	private final MediaFileCleanup fileCleanup;

	public MediaCommandService(
		MediaRepository mediaRepository,
		MediaStorage mediaStorage,
		MediaCollectionPolicy collectionPolicy,
		MediaReadService readService,
		MediaFileCleanup fileCleanup
	) {
		this.mediaRepository = mediaRepository;
		this.mediaStorage = mediaStorage;
		this.collectionPolicy = collectionPolicy;
		this.readService = readService;
		this.fileCleanup = fileCleanup;
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
			softDelete(current);
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
					softDelete(media);
				}
			}
			return readService.toResult(selected);
		}

		if (required) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "필수 미디어 파일을 등록해주세요.");
		}
		softDelete(current);
		return null;
	}

	@Transactional(propagation = Propagation.MANDATORY)
	public List<MediaResult> synchronizeMany(
		MediaOwnerType ownerType,
		Long ownerId,
		String collection,
		List<MediaFileSource> newFiles,
		List<Long> existingMediaIds,
		boolean required,
		int maxCount
	) {
		collectionPolicy.validateCollection(ownerType, collection);
		List<Media> current = lockedCollection(ownerType, ownerId, collection);
		Set<Long> requestedIds = existingMediaIds == null
			? Set.of()
			: existingMediaIds.stream()
				.filter(Objects::nonNull)
				.collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
		Map<Long, Media> currentById = current.stream().collect(java.util.stream.Collectors.toMap(Media::id, media -> media));
		if (!currentById.keySet().containsAll(requestedIds)) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "유지할 미디어 정보가 올바르지 않습니다.");
		}
		List<MediaFileSource> uploads = newFiles == null
			? List.of()
			: newFiles.stream().filter(Objects::nonNull).filter(file -> file.size() > 0).toList();
		int finalCount = requestedIds.size() + uploads.size();
		if (finalCount > maxCount) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "등록 가능한 미디어 개수를 초과했습니다.");
		}
		if (required && finalCount == 0) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "필수 미디어 파일을 등록해주세요.");
		}

		current.forEach(media -> {
			if (!requestedIds.contains(media.id())) {
				softDelete(media);
			}
		});
		int sortOrder = 0;
		for (Long mediaId : requestedIds) {
			Media media = currentById.get(mediaId);
			media.changeSortOrder(sortOrder);
			media.changePrimary(sortOrder == 0);
			sortOrder++;
		}
		for (MediaFileSource upload : uploads) {
			StoredMediaFile stored = store(upload);
			registerRollbackCleanup(stored.path());
			collectionPolicy.validateFile(ownerType, collection, stored);
			Media media = createMedia(ownerType, ownerId, collection, stored, sortOrder, sortOrder == 0, null);
			mediaRepository.save(media);
			sortOrder++;
		}
		mediaRepository.flush();
		return readService.list(ownerType, ownerId, collection);
	}

	@Transactional(propagation = Propagation.MANDATORY)
	public List<MediaResult> synchronizeManyOrdered(
		MediaOwnerType ownerType,
		Long ownerId,
		String collection,
		List<MediaFileSource> newFiles,
		List<String> order,
		boolean required,
		int maxCount
	) {
		collectionPolicy.validateCollection(ownerType, collection);
		List<Media> current = lockedCollection(ownerType, ownerId, collection);
		Map<Long, Media> currentById = current.stream().collect(java.util.stream.Collectors.toMap(Media::id, media -> media));
		List<MediaFileSource> uploads = newFiles == null ? List.of() : newFiles;
		List<OrderedMediaEntry> entries = parseOrder(order, currentById, uploads);
		long uploadedFileCount = uploads.stream()
			.filter(Objects::nonNull)
			.filter(file -> file.size() > 0)
			.count();
		long orderedUploadCount = entries.stream().filter(entry -> entry.newFile() != null).count();
		if (orderedUploadCount != uploadedFileCount) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "새로 업로드한 대표/내부 이미지 순서 정보가 누락되었습니다.");
		}
		if (entries.size() > maxCount) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "등록 가능한 미디어 개수를 초과했습니다.");
		}
		if (required && entries.isEmpty()) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "필수 미디어 파일을 등록해주세요.");
		}

		Set<Long> keptIds = entries.stream()
			.map(OrderedMediaEntry::existing)
			.filter(Objects::nonNull)
			.map(Media::id)
			.collect(java.util.stream.Collectors.toSet());
		current.forEach(media -> {
			if (!keptIds.contains(media.id())) {
				softDelete(media);
			}
		});
		for (int index = 0; index < entries.size(); index++) {
			OrderedMediaEntry entry = entries.get(index);
			if (entry.existing() != null) {
				entry.existing().changeSortOrder(index);
				entry.existing().changePrimary(index == 0);
				continue;
			}
			StoredMediaFile stored = store(entry.newFile());
			registerRollbackCleanup(stored.path());
			collectionPolicy.validateFile(ownerType, collection, stored);
			mediaRepository.save(createMedia(ownerType, ownerId, collection, stored, index, index == 0, null));
		}
		mediaRepository.flush();
		return readService.list(ownerType, ownerId, collection);
	}

	@Transactional(propagation = Propagation.MANDATORY)
	public void synchronizePrimaryAndMany(
		MediaOwnerType ownerType,
		Long ownerId,
		String primaryCollection,
		String manyCollection,
		MediaFileSource newPrimaryFile,
		Long existingPrimaryId,
		List<MediaFileSource> newManyFiles,
		List<String> manyOrder,
		boolean required,
		int maxCount
	) {
		collectionPolicy.validateCollection(ownerType, primaryCollection);
		collectionPolicy.validateCollection(ownerType, manyCollection);
		List<Media> currentPrimary = lockedCollection(ownerType, ownerId, primaryCollection);
		List<Media> currentMany = lockedCollection(ownerType, ownerId, manyCollection);
		Map<Long, Media> currentById = java.util.stream.Stream
			.concat(currentPrimary.stream(), currentMany.stream())
			.collect(java.util.stream.Collectors.toMap(Media::id, media -> media));
		List<MediaFileSource> manyUploads = newManyFiles == null ? List.of() : newManyFiles;
		List<OrderedMediaEntry> manyEntries = parseOrder(manyOrder, currentById, manyUploads);
		Media selectedPrimary = existingPrimaryId == null ? null : currentById.get(existingPrimaryId);
		boolean hasNewPrimary = newPrimaryFile != null && newPrimaryFile.size() > 0;
		if (!hasNewPrimary && existingPrimaryId != null && selectedPrimary == null) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "유지할 대표 이미지 정보가 올바르지 않습니다.");
		}
		if (selectedPrimary != null && manyEntries.stream().anyMatch(entry -> entry.existing() == selectedPrimary)) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "대표 이미지가 업체 이미지 순서에 중복되었습니다.");
		}
		long orderedUploadCount = manyEntries.stream().filter(entry -> entry.newFile() != null).count();
		long uploadedFileCount = manyUploads.stream()
			.filter(Objects::nonNull)
			.filter(file -> file.size() > 0)
			.count();
		if (orderedUploadCount != uploadedFileCount) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "새로 업로드한 업체 이미지 순서 정보가 누락되었습니다.");
		}
		int finalCount = (hasNewPrimary || selectedPrimary != null ? 1 : 0) + manyEntries.size();
		if (finalCount > maxCount) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "등록 가능한 미디어 개수를 초과했습니다.");
		}
		if (required && finalCount == 0) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "필수 미디어 파일을 등록해주세요.");
		}

		Set<Long> keptIds = manyEntries.stream()
			.map(OrderedMediaEntry::existing)
			.filter(Objects::nonNull)
			.map(Media::id)
			.collect(java.util.stream.Collectors.toSet());
		if (!hasNewPrimary && selectedPrimary != null) {
			keptIds.add(selectedPrimary.id());
		}
		currentById.values().forEach(media -> {
			if (!keptIds.contains(media.id())) {
				softDelete(media);
			}
		});

		if (hasNewPrimary) {
			StoredMediaFile stored = store(newPrimaryFile);
			registerRollbackCleanup(stored.path());
			collectionPolicy.validateFile(ownerType, primaryCollection, stored);
			mediaRepository.save(createMedia(
				ownerType, ownerId, primaryCollection, stored, 0, true, null
			));
		} else if (selectedPrimary != null) {
			selectedPrimary.changeCollection(primaryCollection);
			selectedPrimary.changeSortOrder(0);
			selectedPrimary.changePrimary(true);
		}

		for (int index = 0; index < manyEntries.size(); index++) {
			OrderedMediaEntry entry = manyEntries.get(index);
			if (entry.existing() != null) {
				entry.existing().changeCollection(manyCollection);
				entry.existing().changeSortOrder(index);
				entry.existing().changePrimary(index == 0);
				continue;
			}
			StoredMediaFile stored = store(entry.newFile());
			registerRollbackCleanup(stored.path());
			collectionPolicy.validateFile(ownerType, manyCollection, stored);
			mediaRepository.save(createMedia(
				ownerType, ownerId, manyCollection, stored, index, index == 0, null
			));
		}
		mediaRepository.flush();
	}

	private List<OrderedMediaEntry> parseOrder(
		List<String> order,
		Map<Long, Media> currentById,
		List<MediaFileSource> uploads
	) {
		List<String> tokens = order == null ? List.of() : order;
		if (new LinkedHashSet<>(tokens).size() != tokens.size()) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "대표/내부 이미지 순서 정보가 중복되었습니다.");
		}
		List<OrderedMediaEntry> entries = new ArrayList<>();
		Set<Long> existingIds = new LinkedHashSet<>();
		Set<Integer> newIndexes = new LinkedHashSet<>();
		for (String token : tokens) {
			String[] parts = token == null ? new String[0] : token.split(":", 2);
			if (parts.length != 2) {
				throw new ApiException(ErrorCode.INVALID_REQUEST, "대표/내부 이미지 순서 정보가 올바르지 않습니다.");
			}
			try {
				long value = Long.parseLong(parts[1]);
				if ("existing".equals(parts[0])) {
					Media media = currentById.get(value);
					if (media == null || !existingIds.add(value)) {
						throw new ApiException(ErrorCode.INVALID_REQUEST, "선택한 대표/내부 이미지 정보가 올바르지 않습니다.");
					}
					entries.add(new OrderedMediaEntry(media, null));
				} else if (value <= Integer.MAX_VALUE
					&& "new".equals(parts[0])
					&& value >= 0
					&& value < uploads.size()
					&& uploads.get((int) value) != null
					&& uploads.get((int) value).size() > 0
					&& newIndexes.add((int) value)) {
					entries.add(new OrderedMediaEntry(null, uploads.get((int) value)));
				} else {
					throw new ApiException(ErrorCode.INVALID_REQUEST, "새로 업로드한 이미지 순서 정보가 올바르지 않습니다.");
				}
			} catch (NumberFormatException exception) {
				throw new ApiException(ErrorCode.INVALID_REQUEST, "대표/내부 이미지 순서 정보가 올바르지 않습니다.");
			}
		}
		return entries;
	}

	private record OrderedMediaEntry(Media existing, MediaFileSource newFile) {
	}

	private List<Media> lockedCollection(MediaOwnerType ownerType, Long ownerId, String collection) {
		return mediaRepository.findLockedByOwnerTypeAndOwnerIdAndCollectionAndDeletedAtIsNullOrderBySortOrderAscIdAsc(
			ownerType,
			ownerId,
			collection
		);
	}

	@Transactional(propagation = Propagation.MANDATORY)
	public MediaResult append(MediaOwnerType ownerType, Long ownerId, String collection, MediaFileSource file) {
		collectionPolicy.validateCollection(ownerType, collection);
		StoredMediaFile stored = store(file);
		registerRollbackCleanup(stored.path());
		collectionPolicy.validateFile(ownerType, collection, stored);
		return readService.toResult(mediaRepository.saveAndFlush(createMedia(ownerType, ownerId, collection, stored, 0, false, null)));
	}

	private void softDelete(List<Media> media) {
		media.forEach(this::softDelete);
	}

	private void softDelete(Media media) {
		media.softDelete();
		fileCleanup.deleteAfterCommit(List.of(media.path()));
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
			throw toApplicationException(exception);
		}
	}

	private RuntimeException toApplicationException(MediaStorageException exception) {
		return switch (exception.reason()) {
			case INVALID_FILE -> new ApiException(ErrorCode.INVALID_REQUEST, exception.getMessage());
			case FILE_TOO_LARGE -> new ApiException(ErrorCode.PAYLOAD_TOO_LARGE, exception.getMessage());
			case FILE_NOT_FOUND -> new ApiException(ErrorCode.NOT_FOUND, exception.getMessage());
			case IO_ERROR -> {
				yield new InternalApplicationException("미디어 파일 처리 중 오류가 발생했습니다.", exception);
			}
		};
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
