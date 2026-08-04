package com.platform.application.category.definition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.platform.application.category.definition.CategoryTreeDefinition.CategoryNodeDefinition;
import com.platform.domain.category.Category;
import com.platform.domain.category.CategoryDomain;
import com.platform.domain.category.CategoryGroup;
import com.platform.domain.category.CategoryStatus;
import com.platform.domain.category.CategoryUsage;
import com.platform.domain.category.CategoryUsageType;
import com.platform.infrastructure.persistence.category.CategoryRepository;
import com.platform.infrastructure.persistence.category.CategoryUsageRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

class CategoryDefinitionSyncServiceTests {

	private final CategoryDefinitionLoader definitionLoader = mock(CategoryDefinitionLoader.class);
	private final CategoryRepository categoryRepository = mock(CategoryRepository.class);
	private final CategoryUsageRepository categoryUsageRepository = mock(CategoryUsageRepository.class);
	private final CategoryDefinitionSyncService syncService = new CategoryDefinitionSyncService(
		definitionLoader,
		categoryRepository,
		categoryUsageRepository
	);

	@Test
	void synchronizesDefinitionsAndInactivatesRemovedRows() {
		Category existingRoot = category("Old hair name", "KB_HAIR_SALON", null, (byte) 1);
		Category removedCategory = category("Removed", "KB_REMOVED", null, (byte) 1);
		CategoryUsage existingRootUsage = new CategoryUsage(
			CategoryUsageType.PARTNER_CATEGORY,
			existingRoot,
			99,
			CategoryStatus.INACTIVE
		);
		CategoryUsage removedUsage = new CategoryUsage(
			CategoryUsageType.PARTNER_OPTION_CATEGORY,
			removedCategory,
			1,
			CategoryStatus.ACTIVE
		);

		when(definitionLoader.loadPartnerCatalog()).thenReturn(catalog());
		when(categoryRepository.findAllByDomain(eq(CategoryDomain.PARTNER), any(Sort.class)))
			.thenReturn(List.of(existingRoot, removedCategory));
		when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(categoryUsageRepository.findByUsageIn(any()))
			.thenReturn(List.of(existingRootUsage, removedUsage));
		when(categoryUsageRepository.save(any(CategoryUsage.class))).thenAnswer(invocation -> invocation.getArgument(0));

		CategoryDefinitionSyncResult result = syncService.synchronizePartnerDefinitions();

		assertThat(existingRoot.name()).isEqualTo("Hair salon");
		assertThat(existingRoot.sortOrder()).isEqualTo(1);
		assertThat(existingRoot.status()).isEqualTo(CategoryStatus.ACTIVE);
		assertThat(removedCategory.status()).isEqualTo(CategoryStatus.INACTIVE);
		assertThat(existingRootUsage.sortOrder()).isEqualTo(1);
		assertThat(existingRootUsage.status()).isEqualTo(CategoryStatus.ACTIVE);
		assertThat(removedUsage.status()).isEqualTo(CategoryStatus.INACTIVE);
		assertThat(result).isEqualTo(new CategoryDefinitionSyncResult(2, 1, 1, 2, 1, 1));
	}

	private CategoryDefinitionCatalog catalog() {
		CategoryNodeDefinition option = new CategoryNodeDefinition(
			"Cut",
			"KB_HAIR_CUT",
			1,
			List.of()
		);
		CategoryNodeDefinition root = new CategoryNodeDefinition(
			"Hair salon",
			"KB_HAIR_SALON",
			1,
			List.of(option)
		);
		return new CategoryDefinitionCatalog(
			new CategoryTreeDefinition(CategoryDomain.PARTNER, CategoryGroup.TREATMENT, List.of(root))
		);
	}

	private Category category(String name, String code, Category parent, byte depth) {
		return new Category(
			CategoryDomain.PARTNER,
			parent,
			depth,
			CategoryGroup.TREATMENT,
			name,
			code,
			parent == null ? name : parent.fullPath() + " > " + name,
			1,
			CategoryStatus.ACTIVE,
			true
		);
	}
}
