package com.platform.application.category.definition;

import com.platform.application.category.definition.CategoryTreeDefinition.CategoryNodeDefinition;
import com.platform.domain.category.Category;
import com.platform.domain.category.CategoryDomain;
import com.platform.domain.category.CategoryGroup;
import com.platform.domain.category.CategoryStatus;
import com.platform.domain.category.CategoryUsage;
import com.platform.domain.category.CategoryUsageType;
import com.platform.infrastructure.persistence.category.CategoryRepository;
import com.platform.infrastructure.persistence.category.CategoryUsageRepository;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CategoryDefinitionSyncService {

	private static final Set<CategoryUsageType> PARTNER_USAGES = EnumSet.of(
		CategoryUsageType.PARTNER_CATEGORY,
		CategoryUsageType.PARTNER_OPTION_CATEGORY
	);

	private final CategoryDefinitionLoader definitionLoader;
	private final CategoryRepository categoryRepository;
	private final CategoryUsageRepository categoryUsageRepository;

	public CategoryDefinitionSyncService(
		CategoryDefinitionLoader definitionLoader,
		CategoryRepository categoryRepository,
		CategoryUsageRepository categoryUsageRepository
	) {
		this.definitionLoader = definitionLoader;
		this.categoryRepository = categoryRepository;
		this.categoryUsageRepository = categoryUsageRepository;
	}

	@Transactional
	public CategoryDefinitionSyncResult synchronizePartnerDefinitions() {
		CategoryDefinitionCatalog catalog = definitionLoader.loadPartnerCatalog();
		List<Category> existingCategories = categoryRepository.findAllByDomain(
			CategoryDomain.PARTNER,
			Sort.by(Sort.Order.asc("depth"), Sort.Order.asc("sortOrder"), Sort.Order.asc("id"))
		);
		Map<String, Category> existingByCode = indexCategories(existingCategories);
		Map<String, Category> synchronizedByCode = new LinkedHashMap<>();
		SyncCount categoryCount = new SyncCount();

		for (CategoryNodeDefinition root : catalog.rootCategories()) {
			synchronizeCategory(
				root,
				null,
				(byte) 1,
				catalog.tree().groupCode(),
				existingByCode,
				synchronizedByCode,
				categoryCount
			);
		}

		for (Category category : existingCategories) {
			if (!synchronizedByCode.containsKey(category.code()) && category.status() != CategoryStatus.INACTIVE) {
				category.changeStatus(CategoryStatus.INACTIVE);
				categoryCount.inactivated++;
			}
		}

		SyncCount usageCount = synchronizeUsages(catalog, synchronizedByCode);
		return new CategoryDefinitionSyncResult(
			synchronizedByCode.size(),
			categoryCount.created,
			categoryCount.inactivated,
			PARTNER_USAGES.stream().mapToInt(usage -> catalog.categoriesForUsage(usage).size()).sum(),
			usageCount.created,
			usageCount.inactivated
		);
	}

	private void synchronizeCategory(
		CategoryNodeDefinition definition,
		Category parent,
		byte depth,
		CategoryGroup groupCode,
		Map<String, Category> existingByCode,
		Map<String, Category> synchronizedByCode,
		SyncCount count
	) {
		String fullPath = parent == null ? definition.name() : parent.fullPath() + " > " + definition.name();
		Category category = existingByCode.get(definition.code());
		if (category == null) {
			category = categoryRepository.save(new Category(
				CategoryDomain.PARTNER,
				parent,
				depth,
				groupCode,
				definition.name(),
				definition.code(),
				fullPath,
				definition.sortOrder(),
				CategoryStatus.ACTIVE,
				true
			));
			count.created++;
		} else {
			category.synchronizeDefinition(
				parent,
				depth,
				groupCode,
				definition.name(),
				definition.code(),
				fullPath,
				definition.sortOrder(),
				CategoryStatus.ACTIVE,
				true
			);
		}
		synchronizedByCode.put(definition.code(), category);

		for (CategoryNodeDefinition child : definition.children()) {
			synchronizeCategory(
				child,
				category,
				(byte) (depth + 1),
				groupCode,
				existingByCode,
				synchronizedByCode,
				count
			);
		}
	}

	private SyncCount synchronizeUsages(
		CategoryDefinitionCatalog catalog,
		Map<String, Category> synchronizedByCode
	) {
		List<CategoryUsage> existingUsages = categoryUsageRepository.findByUsageIn(PARTNER_USAGES);
		Map<UsageKey, CategoryUsage> existingByKey = indexUsages(existingUsages);
		Set<CategoryUsage> synchronizedUsages = new HashSet<>();
		SyncCount count = new SyncCount();

		for (CategoryUsageType usageType : PARTNER_USAGES) {
			for (CategoryNodeDefinition definition : catalog.categoriesForUsage(usageType)) {
				Category category = synchronizedByCode.get(definition.code());
				UsageKey key = new UsageKey(usageType, definition.code());
				CategoryUsage usage = existingByKey.get(key);
				if (usage == null) {
					usage = categoryUsageRepository.save(new CategoryUsage(
						usageType,
						category,
						definition.sortOrder(),
						CategoryStatus.ACTIVE
					));
					count.created++;
				} else {
					usage.synchronize(definition.sortOrder(), CategoryStatus.ACTIVE);
				}
				synchronizedUsages.add(usage);
			}
		}

		for (CategoryUsage usage : existingUsages) {
			if (!synchronizedUsages.contains(usage) && usage.status() != CategoryStatus.INACTIVE) {
				usage.synchronize(usage.sortOrder(), CategoryStatus.INACTIVE);
				count.inactivated++;
			}
		}
		return count;
	}

	private Map<String, Category> indexCategories(Collection<Category> categories) {
		Map<String, Category> indexed = new HashMap<>();
		for (Category category : categories) {
			if (category.code() == null || category.code().isBlank()) {
				continue;
			}
			Category previous = indexed.putIfAbsent(category.code(), category);
			if (previous != null) {
				throw new IllegalStateException("Duplicate PARTNER category code in database: " + category.code());
			}
		}
		return indexed;
	}

	private Map<UsageKey, CategoryUsage> indexUsages(List<CategoryUsage> usages) {
		Map<UsageKey, CategoryUsage> indexed = new HashMap<>();
		for (CategoryUsage usage : usages) {
			String code = usage.category().code();
			if (code == null || code.isBlank()) {
				continue;
			}
			indexed.put(new UsageKey(usage.usage(), code), usage);
		}
		return indexed;
	}

	private record UsageKey(CategoryUsageType usage, String categoryCode) {
	}

	private static final class SyncCount {

		private int created;
		private int inactivated;
	}
}
