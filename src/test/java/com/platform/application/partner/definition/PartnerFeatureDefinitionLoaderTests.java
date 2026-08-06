package com.platform.application.partner.definition;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.application.partner.definition.PartnerFeatureDefinitionCatalog.PartnerFeatureDefinition;
import java.util.List;
import org.junit.jupiter.api.Test;

class PartnerFeatureDefinitionLoaderTests {

	private final PartnerFeatureDefinitionLoader loader = new PartnerFeatureDefinitionLoader(new ObjectMapper());

	@Test
	void definitionsContainTheCurrentPartnerFeatures() {
		List<PartnerFeatureDefinition> features = loader.load().features();

		assertThat(features).hasSize(17);
		assertThat(features)
			.extracting(PartnerFeatureDefinition::code)
			.doesNotHaveDuplicates()
			.contains("ONE_PERSON_SHOP", "MEN_SERVICE_AVAILABLE")
			.doesNotContain("RESERVATION_ONLY");
		assertThat(features)
			.extracting(PartnerFeatureDefinition::sortOrder)
			.containsExactlyElementsOf(java.util.stream.IntStream.rangeClosed(1, 17).boxed().toList());
	}
}
