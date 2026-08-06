package com.platform.common.config;

import com.platform.application.auth.StaffBootstrapService;
import com.platform.application.auth.command.BootstrapStaffCommand;
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
@EnableConfigurationProperties(StaffSampleBootstrapProperties.class)
class StaffSampleBootstrapConfig {

	private static final String STAFF_ROLE = "platform.staff";
	private static final Logger log = LoggerFactory.getLogger(StaffSampleBootstrapConfig.class);

	@Bean
	@Order(110)
	@ConditionalOnProperty(prefix = "app.bootstrap.staff-sample", name = "enabled", havingValue = "true")
	ApplicationRunner staffSampleBootstrapRunner(
		StaffBootstrapService bootstrapService,
		StaffSampleBootstrapProperties properties
	) {
		return args -> {
			boolean created = bootstrapService.ensureStaff(new BootstrapStaffCommand(
				properties.loginId(),
				properties.email(),
				properties.password(),
				properties.name(),
				properties.nickname(),
				STAFF_ROLE
			));
			log.info("일반 직원 샘플 계정을 확인했습니다. 아이디: {}, 생성 여부: {}", properties.loginId(), created);
		};
	}
}
