package com.platform.application.category.result;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CategoryReferenceResult(
	Long id,
	String name,
	String code,
	@JsonProperty("full_path") String fullPath,
	@JsonProperty("parent_id") Long parentId,
	int depth,
	@JsonProperty("is_primary") boolean primary
) {
}
