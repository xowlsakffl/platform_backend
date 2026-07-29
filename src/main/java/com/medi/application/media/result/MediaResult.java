package com.medi.application.media.result;

import com.medi.domain.media.MediaDisk;
import com.medi.domain.media.MediaOwnerType;
import java.time.LocalDateTime;
import java.util.Map;

public record MediaResult(
	Long id,
	MediaOwnerType ownerType,
	Long ownerId,
	String collection,
	MediaDisk disk,
	String originalName,
	String mimeType,
	long size,
	Integer width,
	Integer height,
	int sortOrder,
	boolean primary,
	Map<String, Object> metadata,
	String contentUrl,
	LocalDateTime createdAt,
	LocalDateTime updatedAt
) {
}
