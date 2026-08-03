package com.platform.common.security;

import com.platform.domain.account.AccountActorType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "app.auth.password-reset")
@Validated
public class PasswordResetProperties {

	@Min(300)
	private long tokenTtlSeconds = 3_600;

	@Min(30)
	private long tokenThrottleSeconds = 60;

	@Min(1)
	private int requestIpMaxAttempts = 10;

	@Min(1)
	private int requestEmailMaxAttempts = 3;

	@Min(60)
	private long requestWindowSeconds = 3_600;

	@Min(1)
	private int verifyMaxAttempts = 30;

	@Min(60)
	private long verifyWindowSeconds = 600;

	@Min(1)
	private int submitMaxAttempts = 10;

	@Min(60)
	private long submitWindowSeconds = 900;

	@NotBlank
	private String mailMode = "smtp";

	@NotBlank
	private String mailFromAddress;

	@NotBlank
	private String mailFromName = "Platform";

	@NotBlank
	private String serviceName = "Platform";

	@NotBlank
	private String staffUrl;

	@NotBlank
	private String partnerUrl;

	@NotBlank
	private String userUrl;

	public long tokenTtlSeconds() {
		return tokenTtlSeconds;
	}

	public void setTokenTtlSeconds(long value) {
		tokenTtlSeconds = value;
	}

	public long tokenThrottleSeconds() {
		return tokenThrottleSeconds;
	}

	public void setTokenThrottleSeconds(long value) {
		tokenThrottleSeconds = value;
	}

	public int requestIpMaxAttempts() {
		return requestIpMaxAttempts;
	}

	public void setRequestIpMaxAttempts(int value) {
		requestIpMaxAttempts = value;
	}

	public int requestEmailMaxAttempts() {
		return requestEmailMaxAttempts;
	}

	public void setRequestEmailMaxAttempts(int value) {
		requestEmailMaxAttempts = value;
	}

	public long requestWindowSeconds() {
		return requestWindowSeconds;
	}

	public void setRequestWindowSeconds(long value) {
		requestWindowSeconds = value;
	}

	public int verifyMaxAttempts() {
		return verifyMaxAttempts;
	}

	public void setVerifyMaxAttempts(int value) {
		verifyMaxAttempts = value;
	}

	public long verifyWindowSeconds() {
		return verifyWindowSeconds;
	}

	public void setVerifyWindowSeconds(long value) {
		verifyWindowSeconds = value;
	}

	public int submitMaxAttempts() {
		return submitMaxAttempts;
	}

	public void setSubmitMaxAttempts(int value) {
		submitMaxAttempts = value;
	}

	public long submitWindowSeconds() {
		return submitWindowSeconds;
	}

	public void setSubmitWindowSeconds(long value) {
		submitWindowSeconds = value;
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

	public String resetUrl(AccountActorType actorType) {
		return switch (actorType) {
			case STAFF -> staffUrl;
			case PARTNER -> partnerUrl;
			case USER -> userUrl;
		};
	}

	public void setStaffUrl(String value) {
		staffUrl = value;
	}

	public void setPartnerUrl(String value) {
		partnerUrl = value;
	}

	public void setUserUrl(String value) {
		userUrl = value;
	}
}
