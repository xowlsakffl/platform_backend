package com.platform.application.category.result;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.platform.domain.category.CategoryDomain;
import com.platform.domain.category.CategoryGroup;
import com.platform.domain.category.CategoryStatus;
import com.platform.application.media.result.MediaResult;
import java.time.LocalDateTime;
import java.util.List;

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
	CategoryResult parent,
	List<CategoryResult> children,
	@JsonProperty("middle_count") Integer middleCount,
	@JsonProperty("small_count") Integer smallCount,
	MediaResult icon,
	@JsonProperty("created_at") LocalDateTime createdAt,
	@JsonProperty("updated_at") LocalDateTime updatedAt
) {
}
