package com.platform.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.bootstrap.partner-sample")
public record PartnerSampleBootstrapProperties(
	boolean enabled,
	String password
) {
}
