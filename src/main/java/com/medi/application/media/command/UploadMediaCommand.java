package com.medi.application.media.command;

import com.medi.application.media.storage.MediaFileSource;
import com.medi.domain.media.MediaOwnerType;

public record UploadMediaCommand(
	MediaOwnerType ownerType,
	Long ownerId,
	String collection,
	Integer sortOrder,
	Boolean primary,
	String metadata,
	MediaFileSource file
) {
}
