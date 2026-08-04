package com.platform.application.category.definition;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.platform.domain.category.CategoryDomain;
import com.platform.domain.category.CategoryGroup;
import java.util.List;

public record CategoryTreeDefinition(
	CategoryDomain domain,
	@JsonProperty("group_code") CategoryGroup groupCode,
	List<CategoryNodeDefinition> categories
) {
	public CategoryTreeDefinition {
		categories = categories == null ? List.of() : List.copyOf(categories);
	}

	public record CategoryNodeDefinition(
		String name,
		String code,
		@JsonProperty("sort_order") int sortOrder,
		List<CategoryNodeDefinition> children
	) {
		public CategoryNodeDefinition {
			children = children == null ? List.of() : List.copyOf(children);
		}
	}
}
