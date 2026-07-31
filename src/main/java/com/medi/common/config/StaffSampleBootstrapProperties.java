package com.medi.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.bootstrap.staff-sample")
public record StaffSampleBootstrapProperties(
	boolean enabled,
	String email,
	String password,
	String name,
	String nickname
) {
}
