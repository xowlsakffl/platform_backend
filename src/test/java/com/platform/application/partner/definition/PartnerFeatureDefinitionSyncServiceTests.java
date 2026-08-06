package com.platform.application.partner.definition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.platform.application.partner.definition.PartnerFeatureDefinitionCatalog.PartnerFeatureDefinition;
import com.platform.domain.partner.PartnerFeature;
import com.platform.domain.partner.PartnerFeatureStatus;
import com.platform.infrastructure.persistence.partner.PartnerFeatureRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

class PartnerFeatureDefinitionSyncServiceTests {

	private final PartnerFeatureDefinitionLoader definitionLoader = mock(PartnerFeatureDefinitionLoader.class);
	private final PartnerFeatureRepository featureRepository = mock(PartnerFeatureRepository.class);
	private final PartnerFeatureDefinitionSyncService syncService = new PartnerFeatureDefinitionSyncService(
		definitionLoader,
		featureRepository
	);

	@Test
	void synchronizesDefinitionsAndInactivatesRemovedRows() {
		PartnerFeature parking = new PartnerFeature("PARKING", "old name", 99, PartnerFeatureStatus.INACTIVE);
		PartnerFeature removed = new PartnerFeature("REMOVED", "removed", 2, PartnerFeatureStatus.ACTIVE);
		PartnerFeatureDefinition definition = new PartnerFeatureDefinition("PARKING", "주차 가능", 1);

		when(definitionLoader.load()).thenReturn(new PartnerFeatureDefinitionCatalog(List.of(definition)));
		when(featureRepository.findAll(any(Sort.class))).thenReturn(List.of(parking, removed));
		when(featureRepository.save(any(PartnerFeature.class))).thenAnswer(invocation -> invocation.getArgument(0));

		PartnerFeatureDefinitionSyncResult result = syncService.synchronize();

		assertThat(parking.name()).isEqualTo("주차 가능");
		assertThat(parking.sortOrder()).isEqualTo(1);
		assertThat(parking.status()).isEqualTo(PartnerFeatureStatus.ACTIVE);
		assertThat(removed.status()).isEqualTo(PartnerFeatureStatus.INACTIVE);
		assertThat(result).isEqualTo(new PartnerFeatureDefinitionSyncResult(1, 0, 1));
	}
}
