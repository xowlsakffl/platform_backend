package com.platform.application.media;

import com.platform.application.media.result.MediaContentResult;
import com.platform.application.media.storage.MediaContent;
import com.platform.application.media.storage.MediaStorage;
import com.platform.application.media.storage.MediaStorageException;
import com.platform.common.error.ApiException;
import com.platform.common.error.ErrorCode;
import com.platform.common.error.InternalApplicationException;
import com.platform.domain.media.Media;
import com.platform.infrastructure.persistence.media.MediaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MediaContentForUserService {

	private final MediaOwnerPolicy ownerPolicy;
	private final MediaRepository mediaRepository;
	private final MediaStorage mediaStorage;

	public MediaContentForUserService(
		MediaOwnerPolicy ownerPolicy,
		MediaRepository mediaRepository,
		MediaStorage mediaStorage
	) {
		this.ownerPolicy = ownerPolicy;
		this.mediaRepository = mediaRepository;
		this.mediaStorage = mediaStorage;
	}

	@Transactional(readOnly = true)
	public MediaContentResult content(Long id) {
		Media media = mediaRepository.findByIdAndDeletedAtIsNull(id)
			.orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "미디어를 찾을 수 없습니다."));
		ownerPolicy.requireAppReadable(media);
		MediaContent content = loadContent(media.path());
		return new MediaContentResult(media.originalName(), media.mimeType(), content.contentLength(), content);
	}

	private MediaContent loadContent(String path) {
		try {
			return mediaStorage.load(path);
		} catch (MediaStorageException exception) {
			throw switch (exception.reason()) {
				case INVALID_FILE, FILE_TOO_LARGE -> new ApiException(ErrorCode.INVALID_REQUEST, exception.getMessage());
				case FILE_NOT_FOUND -> new ApiException(ErrorCode.NOT_FOUND, exception.getMessage());
				case IO_ERROR -> new InternalApplicationException("미디어 파일 조회 중 오류가 발생했습니다.", exception);
			};
		}
	}
}
