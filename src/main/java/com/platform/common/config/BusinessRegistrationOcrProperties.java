package com.platform.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@ConfigurationProperties(prefix = "app.ocr.business-registration")
public class BusinessRegistrationOcrProperties {

	private boolean enabled;
	private String invokeUrl;
	private String secret;
	private double confirmationConfidence = 0.8;

	public boolean enabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public String invokeUrl() {
		return invokeUrl;
	}

	public void setInvokeUrl(String invokeUrl) {
		this.invokeUrl = invokeUrl;
	}

	public String secret() {
		return secret;
	}

	public void setSecret(String secret) {
		this.secret = secret;
	}

	public double confirmationConfidence() {
		return confirmationConfidence;
	}

	public void setConfirmationConfidence(double confirmationConfidence) {
		this.confirmationConfidence = confirmationConfidence;
	}

	public boolean configured() {
		return enabled && StringUtils.hasText(invokeUrl) && StringUtils.hasText(secret);
	}
}
