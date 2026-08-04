package com.platform.application.category.definition;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.application.category.definition.CategoryTreeDefinition.CategoryNodeDefinition;
import com.platform.domain.category.CategoryDomain;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Component
public class CategoryDefinitionLoader {

	private static final String PARTNER_TREE = "category-definitions/trees/partner.json";

	private final ObjectMapper objectMapper;

	public CategoryDefinitionLoader(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	public CategoryDefinitionCatalog loadPartnerCatalog() {
		CategoryTreeDefinition tree = read(PARTNER_TREE, CategoryTreeDefinition.class);
		CategoryDefinitionCatalog catalog = new CategoryDefinitionCatalog(tree);
		validatePartnerCatalog(catalog);
		return catalog;
	}

	private <T> T read(String path, Class<T> type) {
		ClassPathResource resource = new ClassPathResource(path);
		if (!resource.exists()) {
			throw new IllegalStateException("Category definition does not exist: " + path);
		}
		try (InputStream input = resource.getInputStream()) {
			return objectMapper.readValue(input, type);
		} catch (IOException exception) {
			throw new IllegalStateException("Category definition cannot be read: " + path, exception);
		}
	}

	private void validatePartnerCatalog(CategoryDefinitionCatalog catalog) {
		CategoryTreeDefinition tree = catalog.tree();
		if (tree == null || tree.domain() != CategoryDomain.PARTNER) {
			throw invalid("Partner tree domain must be PARTNER.");
		}
		if (tree.groupCode() == null || tree.categories() == null || tree.categories().isEmpty()) {
			throw invalid("Partner tree group and categories are required.");
		}

		Map<String, Integer> depthByCode = new HashMap<>();
		Set<String> siblingKeys = new HashSet<>();
		List<CategoryNodeDefinition> roots = tree.categories();
		for (CategoryNodeDefinition root : roots) {
			collect(root, null, 1, depthByCode, siblingKeys);
		}

	}

	private void collect(
		CategoryNodeDefinition node,
		String parentCode,
		int depth,
		Map<String, Integer> depthByCode,
		Set<String> siblingKeys
	) {
		if (depth > 2) {
			throw invalid("Partner category depth cannot exceed 2: " + node.code());
		}
		if (node.name() == null || node.name().isBlank() || node.code() == null || node.code().isBlank()) {
			throw invalid("Category name and code are required.");
		}
		if (node.sortOrder() < 0) {
			throw invalid("Category sort_order cannot be negative: " + node.code());
		}
		if (depthByCode.putIfAbsent(node.code(), depth) != null) {
			throw invalid("Duplicate category code: " + node.code());
		}
		String siblingKey = (parentCode == null ? "ROOT" : parentCode) + "\u0000" + node.name();
		if (!siblingKeys.add(siblingKey)) {
			throw invalid("Duplicate sibling category name: " + node.name());
		}
		for (CategoryNodeDefinition child : node.children()) {
			collect(child, node.code(), depth + 1, depthByCode, siblingKeys);
		}
	}

	private IllegalStateException invalid(String message) {
		return new IllegalStateException("Invalid category definition. " + message);
	}
}
