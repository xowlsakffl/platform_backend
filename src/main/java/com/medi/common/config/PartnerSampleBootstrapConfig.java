package com.medi.common.config;

import com.medi.application.partner.PartnerSampleBootstrapService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;

@Configuration
@Profile("local")
@EnableConfigurationProperties(PartnerSampleBootstrapProperties.class)
class PartnerSampleBootstrapConfig {

	private static final Logger log = LoggerFactory.getLogger(PartnerSampleBootstrapConfig.class);

	@Bean
	@Order(200)
	@ConditionalOnProperty(prefix = "app.bootstrap.partner-sample", name = "enabled", havingValue = "true")
	ApplicationRunner partnerSampleBootstrapRunner(
		PartnerSampleBootstrapService bootstrapService,
		PartnerSampleBootstrapProperties properties
	) {
		return args -> {
			int createdCount = bootstrapService.ensureSamples(properties.password());
			log.info("파트너 샘플 데이터 초기화를 완료했습니다. 생성: {}, 유지: {}", createdCount, 8 - createdCount);
		};
	}
}
