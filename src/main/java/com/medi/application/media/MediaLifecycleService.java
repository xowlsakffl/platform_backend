package com.medi.application.media;

import com.medi.domain.media.Media;
import com.medi.domain.media.MediaOwnerType;
import com.medi.infrastructure.persistence.media.MediaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MediaLifecycleService {

	private final MediaRepository mediaRepository;

	public MediaLifecycleService(MediaRepository mediaRepository) {
		this.mediaRepository = mediaRepository;
	}

	@Transactional(propagation = Propagation.MANDATORY)
	public void softDeleteOwnedMedia(MediaOwnerType ownerType, Long ownerId) {
		for (Media media : mediaRepository.findByOwnerTypeAndOwnerIdAndDeletedAtIsNull(ownerType, ownerId)) {
			media.softDelete();
		}
	}
}
