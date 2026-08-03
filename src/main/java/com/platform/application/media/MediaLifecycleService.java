package com.platform.application.media;

import com.platform.domain.media.Media;
import com.platform.domain.media.MediaOwnerType;
import com.platform.infrastructure.persistence.media.MediaRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MediaLifecycleService {

	private final MediaRepository mediaRepository;
	private final MediaFileCleanup fileCleanup;

	public MediaLifecycleService(MediaRepository mediaRepository, MediaFileCleanup fileCleanup) {
		this.mediaRepository = mediaRepository;
		this.fileCleanup = fileCleanup;
	}

	@Transactional(propagation = Propagation.MANDATORY)
	public void softDeleteOwnedMedia(MediaOwnerType ownerType, Long ownerId) {
		List<Media> mediaItems = mediaRepository.findByOwnerTypeAndOwnerIdAndDeletedAtIsNull(ownerType, ownerId);
		for (Media media : mediaItems) {
			media.softDelete();
		}
		fileCleanup.deleteAfterCommit(mediaItems.stream().map(Media::path).toList());
	}
}
