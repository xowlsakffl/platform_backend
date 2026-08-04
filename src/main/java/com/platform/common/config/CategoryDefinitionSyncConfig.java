package com.platform.common.config;

import com.platform.application.category.definition.CategoryDefinitionSyncResult;
import com.platform.application.category.definition.CategoryDefinitionSyncService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

@Configuration
class CategoryDefinitionSyncConfig {

	private static final Logger log = LoggerFactory.getLogger(CategoryDefinitionSyncConfig.class);

	@Bean
	@Order(50)
	@ConditionalOnProperty(
		prefix = "app.category-definitions",
		name = "sync-enabled",
		havingValue = "true",
		matchIfMissing = true
	)
	ApplicationRunner categoryDefinitionSyncRunner(CategoryDefinitionSyncService syncService) {
		return args -> {
			CategoryDefinitionSyncResult result = syncService.synchronizePartnerDefinitions();
			log.info(
				"Category definitions synchronized. categories: {} (created: {}, inactivated: {}), "
					+ "usages: {} (created: {}, inactivated: {})",
				result.definedCategories(),
				result.createdCategories(),
				result.inactivatedCategories(),
				result.definedUsages(),
				result.createdUsages(),
				result.inactivatedUsages()
			);
		};
	}
}
