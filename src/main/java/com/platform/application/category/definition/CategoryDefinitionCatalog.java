package com.platform.application.category.definition;

import com.platform.domain.category.CategoryUsageType;
import java.util.List;

public record CategoryDefinitionCatalog(
	CategoryTreeDefinition tree
) {

	public List<CategoryTreeDefinition.CategoryNodeDefinition> rootCategories() {
		return tree.categories();
	}

	public List<CategoryTreeDefinition.CategoryNodeDefinition> categoriesForUsage(CategoryUsageType usage) {
		return switch (usage) {
			case PARTNER_CATEGORY -> rootCategories();
			case PARTNER_OPTION_CATEGORY -> rootCategories().stream()
				.flatMap(category -> category.children().stream())
				.toList();
			default -> throw new IllegalArgumentException("Unsupported partner category usage: " + usage);
		};
	}
}
