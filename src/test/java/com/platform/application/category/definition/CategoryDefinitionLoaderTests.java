package com.platform.application.category.definition;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.application.category.definition.CategoryTreeDefinition.CategoryNodeDefinition;
import com.platform.domain.category.CategoryDomain;
import com.platform.domain.category.CategoryUsageType;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class CategoryDefinitionLoaderTests {

	private final CategoryDefinitionLoader loader = new CategoryDefinitionLoader(new ObjectMapper());

	@Test
	void partnerDefinitionsContainTheCurrentTreeAndUsages() {
		CategoryDefinitionCatalog catalog = loader.loadPartnerCatalog();
		List<CategoryNodeDefinition> roots = catalog.rootCategories();
		List<CategoryNodeDefinition> options = roots.stream()
			.flatMap(root -> root.children().stream())
			.toList();

		assertThat(catalog.tree().domain()).isEqualTo(CategoryDomain.PARTNER);
		assertThat(roots).hasSize(10);
		assertThat(options).hasSize(54);
		assertThat(catalog.categoriesForUsage(CategoryUsageType.PARTNER_CATEGORY))
			.hasSize(10)
			.extracting(CategoryNodeDefinition::code)
			.doesNotContain("KB_OTHER");
		assertThat(catalog.categoriesForUsage(CategoryUsageType.PARTNER_OPTION_CATEGORY)).hasSize(54);
		assertThat(Stream.concat(roots.stream(), options.stream()).map(CategoryNodeDefinition::code))
			.doesNotHaveDuplicates();
	}
}
