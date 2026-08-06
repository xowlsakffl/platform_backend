package com.platform.application.partner.definition;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record PartnerFeatureDefinitionCatalog(
	List<PartnerFeatureDefinition> features
) {
	public PartnerFeatureDefinitionCatalog {
		features = features == null ? List.of() : List.copyOf(features);
	}

	public record PartnerFeatureDefinition(
		String code,
		String name,
		@JsonProperty("sort_order") int sortOrder
	) {
	}
}
