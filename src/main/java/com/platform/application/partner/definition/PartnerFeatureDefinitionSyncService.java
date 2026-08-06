package com.platform.application.partner.definition;

import com.platform.application.partner.definition.PartnerFeatureDefinitionCatalog.PartnerFeatureDefinition;
import com.platform.domain.partner.PartnerFeature;
import com.platform.domain.partner.PartnerFeatureStatus;
import com.platform.infrastructure.persistence.partner.PartnerFeatureRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PartnerFeatureDefinitionSyncService {

	private final PartnerFeatureDefinitionLoader definitionLoader;
	private final PartnerFeatureRepository featureRepository;

	public PartnerFeatureDefinitionSyncService(
		PartnerFeatureDefinitionLoader definitionLoader,
		PartnerFeatureRepository featureRepository
	) {
		this.definitionLoader = definitionLoader;
		this.featureRepository = featureRepository;
	}

	@Transactional
	public PartnerFeatureDefinitionSyncResult synchronize() {
		List<PartnerFeatureDefinition> definitions = definitionLoader.load().features();
		List<PartnerFeature> existingFeatures = featureRepository.findAll(
			Sort.by(Sort.Order.asc("sortOrder"), Sort.Order.asc("id"))
		);
		Map<String, PartnerFeature> existingByCode = indexByCode(existingFeatures);
		Set<String> definedCodes = definitions.stream()
			.map(PartnerFeatureDefinition::code)
			.collect(Collectors.toSet());
		int created = 0;
		int inactivated = 0;

		for (PartnerFeatureDefinition definition : definitions) {
			PartnerFeature feature = existingByCode.get(definition.code());
			if (feature == null) {
				featureRepository.save(new PartnerFeature(
					definition.code(),
					definition.name(),
					definition.sortOrder(),
					PartnerFeatureStatus.ACTIVE
				));
				created++;
			} else {
				feature.synchronizeDefinition(
					definition.name(),
					definition.sortOrder(),
					PartnerFeatureStatus.ACTIVE
				);
			}
		}

		for (PartnerFeature feature : existingFeatures) {
			if (!definedCodes.contains(feature.code()) && feature.status() != PartnerFeatureStatus.INACTIVE) {
				feature.synchronizeDefinition(
					feature.name(),
					feature.sortOrder(),
					PartnerFeatureStatus.INACTIVE
				);
				inactivated++;
			}
		}

		return new PartnerFeatureDefinitionSyncResult(definitions.size(), created, inactivated);
	}

	private Map<String, PartnerFeature> indexByCode(List<PartnerFeature> features) {
		Map<String, PartnerFeature> indexed = new HashMap<>();
		for (PartnerFeature feature : features) {
			PartnerFeature previous = indexed.putIfAbsent(feature.code(), feature);
			if (previous != null) {
				throw new IllegalStateException("Duplicate partner feature code in database: " + feature.code());
			}
		}
		return indexed;
	}
}
