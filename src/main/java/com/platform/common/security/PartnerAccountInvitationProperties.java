package com.platform.common.security;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "app.partner-account-invitation")
@Validated
public class PartnerAccountInvitationProperties {

	@Min(300)
	private long tokenTtlSeconds = 259_200;

	@NotBlank
	private String frontendUrl = "http://localhost:4003/account/setup";

	@NotBlank
	private String mailMode = "smtp";

	@NotBlank
	private String mailFromAddress = "no-reply@platform.local";

	@NotBlank
	private String mailFromName = "Platform";

	@NotBlank
	private String serviceName = "Platform";

	public long tokenTtlSeconds() {
		return tokenTtlSeconds;
	}

	public void setTokenTtlSeconds(long value) {
		tokenTtlSeconds = value;
	}

	public String frontendUrl() {
		return frontendUrl;
	}

	public void setFrontendUrl(String value) {
		frontendUrl = value;
	}

	public String mailMode() {
		return mailMode;
	}

	public void setMailMode(String value) {
		mailMode = value;
	}

	public String mailFromAddress() {
		return mailFromAddress;
	}

	public void setMailFromAddress(String value) {
		mailFromAddress = value;
	}

	public String mailFromName() {
		return mailFromName;
	}

	public void setMailFromName(String value) {
		mailFromName = value;
	}

	public String serviceName() {
		return serviceName;
	}

	public void setServiceName(String value) {
		serviceName = value;
	}
}
