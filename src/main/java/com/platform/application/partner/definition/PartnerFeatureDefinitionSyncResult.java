package com.platform.application.partner.definition;

public record PartnerFeatureDefinitionSyncResult(
	int definedFeatures,
	int createdFeatures,
	int inactivatedFeatures
) {
}
