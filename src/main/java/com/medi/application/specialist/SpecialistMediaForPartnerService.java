package com.medi.application.specialist;

import com.medi.application.auth.OwnershipPolicy;
import com.medi.application.media.MediaCollectionPolicy;
import com.medi.application.media.result.MediaContentResult;
import com.medi.application.media.storage.MediaContent;
import com.medi.application.media.storage.MediaStorage;
import com.medi.application.media.storage.MediaStorageException;
import com.medi.common.error.ApiException;
import com.medi.common.error.ErrorCode;
import com.medi.common.error.InternalApplicationException;
import com.medi.common.security.AuthenticatedActor;
import com.medi.domain.media.Media;
import com.medi.domain.media.MediaOwnerType;
import com.medi.infrastructure.persistence.specialist.SpecialistRepository;
import com.medi.infrastructure.persistence.media.MediaRepository;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SpecialistMediaForPartnerService {

	private static final Set<String> SPECIALIST_COLLECTIONS = Set.of(
		MediaCollectionPolicy.SPECIALIST_PROFILE_IMAGE,
		MediaCollectionPolicy.SPECIALIST_LICENSE_IMAGE,
		MediaCollectionPolicy.SPECIALIST_SPECIALIST_CERTIFICATE_IMAGE
	);

	private final OwnershipPolicy ownershipPolicy;
	private final SpecialistRepository specialistRepository;
	private final MediaRepository mediaRepository;
	private final MediaStorage mediaStorage;

	public SpecialistMediaForPartnerService(
		OwnershipPolicy ownershipPolicy,
		SpecialistRepository specialistRepository,
		MediaRepository mediaRepository,
		MediaStorage mediaStorage
	) {
		this.ownershipPolicy = ownershipPolicy;
		this.specialistRepository = specialistRepository;
		this.mediaRepository = mediaRepository;
		this.mediaStorage = mediaStorage;
	}

	@Transactional(readOnly = true)
	public MediaContentResult content(AuthenticatedActor actor, Long specialistId, Long mediaId) {
		Long partnerId = actor == null ? null : actor.partnerId();
		ownershipPolicy.requirePartnerOwner(actor, partnerId);
		if (partnerId == null) {
			throw new ApiException(ErrorCode.FORBIDDEN);
		}
		specialistRepository
			.findByIdAndPartner_IdAndDeletedAtIsNullAndPartner_DeletedAtIsNull(specialistId, partnerId)
			.orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "스페셜리스트을 찾을 수 없습니다."));

		Media media = mediaRepository.findByIdAndDeletedAtIsNull(mediaId)
			.filter(item -> item.ownerType() == MediaOwnerType.SPECIALIST)
			.filter(item -> specialistId.equals(item.ownerId()))
			.filter(item -> SPECIALIST_COLLECTIONS.contains(item.collection()))
			.orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "미디어를 찾을 수 없습니다."));
		MediaContent content = loadContent(media.path());
		return new MediaContentResult(media.originalName(), media.mimeType(), content.contentLength(), content);
	}

	private MediaContent loadContent(String path) {
		try {
			return mediaStorage.load(path);
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
}
