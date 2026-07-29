package com.medi.application.media;

import com.medi.application.media.result.MediaContentResult;
import com.medi.application.media.storage.MediaContent;
import com.medi.application.media.storage.MediaStorage;
import com.medi.application.media.storage.MediaStorageException;
import com.medi.common.error.ApiException;
import com.medi.common.error.ErrorCode;
import com.medi.common.error.InternalApplicationException;
import com.medi.common.security.AuthenticatedActor;
import com.medi.domain.media.Media;
import com.medi.infrastructure.persistence.media.MediaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MediaContentForStaffService {

	private final MediaOwnerPolicy ownerPolicy;
	private final MediaRepository mediaRepository;
	private final MediaStorage mediaStorage;

	public MediaContentForStaffService(
		MediaOwnerPolicy ownerPolicy,
		MediaRepository mediaRepository,
		MediaStorage mediaStorage
	) {
		this.ownerPolicy = ownerPolicy;
		this.mediaRepository = mediaRepository;
		this.mediaStorage = mediaStorage;
	}

	@Transactional(readOnly = true)
	public MediaContentResult content(AuthenticatedActor actor, Long id) {
		Media media = mediaRepository.findByIdAndDeletedAtIsNull(id)
			.orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "미디어를 찾을 수 없습니다."));
		ownerPolicy.requireReadable(actor, media.ownerType(), media.ownerId());
		MediaContent content = loadContent(media.path());
		return new MediaContentResult(media.originalName(), media.mimeType(), content.contentLength(), content);
	}

	private MediaContent loadContent(String path) {
		try {
			return mediaStorage.load(path);
		} catch (MediaStorageException exception) {
			throw switch (exception.reason()) {
				case INVALID_FILE, FILE_TOO_LARGE -> new ApiException(
					ErrorCode.INVALID_REQUEST,
					exception.getMessage()
				);
				case FILE_NOT_FOUND -> new ApiException(ErrorCode.NOT_FOUND, exception.getMessage());
				case IO_ERROR -> new InternalApplicationException(
					"미디어 파일 조회 중 오류가 발생했습니다.",
					exception
				);
			};
		}
	}
}
