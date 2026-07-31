package com.medi.common.security;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "app.auth.login-attempt")
@Validated
public class LoginAttemptProperties {

	@Min(1)
	private int maxAttempts = 5;

	@Min(1)
	private int accountMaxAttempts = 15;

	@Min(60)
	private long windowSeconds = 600;

	public int maxAttempts() {
		return maxAttempts;
	}

	public void setMaxAttempts(int maxAttempts) {
		this.maxAttempts = maxAttempts;
	}

	public int accountMaxAttempts() {
		return accountMaxAttempts;
	}

	public void setAccountMaxAttempts(int accountMaxAttempts) {
		this.accountMaxAttempts = accountMaxAttempts;
	}

	public long windowSeconds() {
		return windowSeconds;
	}

	public void setWindowSeconds(long windowSeconds) {
		this.windowSeconds = windowSeconds;
	}
}
