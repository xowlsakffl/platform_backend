package com.medi.common.config;

import com.medi.application.auth.StaffBootstrapService;
import com.medi.application.auth.command.BootstrapStaffCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

@Configuration
@EnableConfigurationProperties(StaffBootstrapProperties.class)
class StaffBootstrapConfig {

	private static final Logger log = LoggerFactory.getLogger(StaffBootstrapConfig.class);

	@Bean
	@Order(100)
	@ConditionalOnProperty(prefix = "app.bootstrap.staff", name = "enabled", havingValue = "true")
	ApplicationRunner staffBootstrapRunner(
		StaffBootstrapService bootstrapService,
		StaffBootstrapProperties properties
	) {
		return args -> {
			boolean created = bootstrapService.ensureStaff(new BootstrapStaffCommand(
				properties.email(),
				properties.password(),
				properties.name(),
				properties.nickname(),
				properties.role()
			));

			if (created) {
				log.info("초기 운영자 계정을 생성했습니다: {}", properties.email());
			} else {
				log.info("기존 운영자 계정의 역할을 확인했습니다: {}", properties.email());
			}
		};
	}
}
