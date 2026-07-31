package com.medi.application.partner.result;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PartnerCategoryResult(
	Long id,
	String domain,
	@JsonProperty("parent_id") Long parentId,
	String name,
	@JsonProperty("full_path") String fullPath,
	int depth,
	@JsonProperty("sort_order") int sortOrder
) {
}
