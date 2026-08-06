package com.platform.common.config;

import com.platform.application.partner.definition.PartnerFeatureDefinitionSyncResult;
import com.platform.application.partner.definition.PartnerFeatureDefinitionSyncService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

@Configuration
class PartnerFeatureDefinitionSyncConfig {

	private static final Logger log = LoggerFactory.getLogger(PartnerFeatureDefinitionSyncConfig.class);

	@Bean
	@Order(60)
	@ConditionalOnProperty(
		prefix = "app.partner-feature-definitions",
		name = "sync-enabled",
		havingValue = "true",
		matchIfMissing = true
	)
	ApplicationRunner partnerFeatureDefinitionSyncRunner(PartnerFeatureDefinitionSyncService syncService) {
		return args -> {
			PartnerFeatureDefinitionSyncResult result = syncService.synchronize();
			log.info(
				"Partner feature definitions synchronized. features: {} (created: {}, inactivated: {})",
				result.definedFeatures(),
				result.createdFeatures(),
				result.inactivatedFeatures()
			);
		};
	}
}
