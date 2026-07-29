package com.medi.application.media.result;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.medi.domain.media.MediaDisk;
import com.medi.domain.media.MediaOwnerType;
import java.time.LocalDateTime;
import java.util.Map;

public record MediaResult(
	Long id,
	@JsonProperty("owner_type") MediaOwnerType ownerType,
	@JsonProperty("owner_id") Long ownerId,
	String collection,
	MediaDisk disk,
	@JsonProperty("original_name") String originalName,
	@JsonProperty("mime_type") String mimeType,
	long size,
	Integer width,
	Integer height,
	@JsonProperty("sort_order") int sortOrder,
	@JsonProperty("is_primary") boolean primary,
	Map<String, Object> metadata,
	@JsonProperty("content_url") String contentUrl,
	@JsonProperty("created_at") LocalDateTime createdAt,
	@JsonProperty("updated_at") LocalDateTime updatedAt
) {
}
