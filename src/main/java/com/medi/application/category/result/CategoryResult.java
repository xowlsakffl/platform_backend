package com.medi.application.category.result;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.medi.domain.category.CategoryDomain;
import com.medi.domain.category.CategoryGroup;
import com.medi.domain.category.CategoryStatus;
import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record CategoryResult(
	Long id,
	CategoryDomain domain,
	@JsonProperty("parent_id") Long parentId,
	int depth,
	@JsonProperty("group_code") CategoryGroup groupCode,
	@JsonProperty("group_label") String groupLabel,
	String name,
	String code,
	@JsonProperty("full_path") String fullPath,
	@JsonProperty("sort_order") int sortOrder,
	CategoryStatus status,
	@JsonProperty("is_menu_visible") boolean menuVisible,
	@JsonProperty("has_children") boolean hasChildren,
	@JsonProperty("middle_count") Integer middleCount,
	@JsonProperty("small_count") Integer smallCount,
	@JsonProperty("created_at") LocalDateTime createdAt,
	@JsonProperty("updated_at") LocalDateTime updatedAt
) {
}
