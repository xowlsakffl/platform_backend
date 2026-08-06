package com.platform.application.partner.definition;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.application.partner.definition.PartnerFeatureDefinitionCatalog.PartnerFeatureDefinition;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Component
public class PartnerFeatureDefinitionLoader {

	private static final String DEFINITION_PATH = "partner-feature-definitions/features.json";
	private static final Pattern CODE_PATTERN = Pattern.compile("[A-Z][A-Z0-9_]*");

	private final ObjectMapper objectMapper;

	public PartnerFeatureDefinitionLoader(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	public PartnerFeatureDefinitionCatalog load() {
		ClassPathResource resource = new ClassPathResource(DEFINITION_PATH);
		if (!resource.exists()) {
			throw new IllegalStateException("Partner feature definition does not exist: " + DEFINITION_PATH);
		}
		try (InputStream input = resource.getInputStream()) {
			PartnerFeatureDefinitionCatalog catalog = objectMapper.readValue(
				input,
				PartnerFeatureDefinitionCatalog.class
			);
			validate(catalog);
			return catalog;
		} catch (IOException exception) {
			throw new IllegalStateException("Partner feature definition cannot be read: " + DEFINITION_PATH, exception);
		}
	}

	private void validate(PartnerFeatureDefinitionCatalog catalog) {
		if (catalog == null || catalog.features().isEmpty()) {
			throw invalid("At least one feature is required.");
		}
		Set<String> codes = new HashSet<>();
		Set<Integer> sortOrders = new HashSet<>();
		for (PartnerFeatureDefinition feature : catalog.features()) {
			if (feature.code() == null || !CODE_PATTERN.matcher(feature.code()).matches()) {
				throw invalid("Feature code must use upper snake case: " + feature.code());
			}
			if (feature.name() == null || feature.name().isBlank()) {
				throw invalid("Feature name is required: " + feature.code());
			}
			if (feature.sortOrder() < 0) {
				throw invalid("Feature sort_order cannot be negative: " + feature.code());
			}
			if (!codes.add(feature.code())) {
				throw invalid("Duplicate feature code: " + feature.code());
			}
			if (!sortOrders.add(feature.sortOrder())) {
				throw invalid("Duplicate feature sort_order: " + feature.sortOrder());
			}
		}
	}

	private IllegalStateException invalid(String message) {
		return new IllegalStateException("Invalid partner feature definition. " + message);
	}
}
