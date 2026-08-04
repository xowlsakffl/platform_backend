package com.platform.application.category.definition;

public record CategoryDefinitionSyncResult(
	int definedCategories,
	int createdCategories,
	int inactivatedCategories,
	int definedUsages,
	int createdUsages,
	int inactivatedUsages
) {
}
