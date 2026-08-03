package com.platform.common.security;

import com.platform.domain.account.AccountActorType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "app.auth.session")
@Validated
public class AuthSessionProperties {

	private boolean cookieSecure = true;

	@NotBlank
	private String cookieSameSite = "Strict";

	@Min(32)
	@Max(64)
	private int refreshTokenBytes = 32;

	@Min(5)
	@Max(120)
	private long rotationGraceSeconds = 30;

	@Min(900)
	private long staffTtlSeconds = 28_800;

	@Min(900)
	private long staffPersistentTtlSeconds = 604_800;

	@Min(900)
	private long partnerTtlSeconds = 43_200;

	@Min(900)
	private long partnerPersistentTtlSeconds = 2_592_000;

	@Min(900)
	private long userTtlSeconds = 86_400;

	@Min(900)
	private long userPersistentTtlSeconds = 7_776_000;

	@NotBlank
	private String cleanupCron = "0 20 4 * * *";

	public boolean cookieSecure() {
		return cookieSecure;
	}

	public void setCookieSecure(boolean cookieSecure) {
		this.cookieSecure = cookieSecure;
	}

	public String cookieSameSite() {
		return cookieSameSite;
	}

	public void setCookieSameSite(String cookieSameSite) {
		this.cookieSameSite = cookieSameSite;
	}

	public int refreshTokenBytes() {
		return refreshTokenBytes;
	}

	public void setRefreshTokenBytes(int refreshTokenBytes) {
		this.refreshTokenBytes = refreshTokenBytes;
	}

	public long rotationGraceSeconds() {
		return rotationGraceSeconds;
	}

	public void setRotationGraceSeconds(long rotationGraceSeconds) {
		this.rotationGraceSeconds = rotationGraceSeconds;
	}

	public long ttlSeconds(AccountActorType actorType, boolean persistent) {
		return switch (actorType) {
			case STAFF -> persistent ? staffPersistentTtlSeconds : staffTtlSeconds;
			case PARTNER -> persistent ? partnerPersistentTtlSeconds : partnerTtlSeconds;
			case USER -> persistent ? userPersistentTtlSeconds : userTtlSeconds;
		};
	}

	public long getStaffTtlSeconds() {
		return staffTtlSeconds;
	}

	public void setStaffTtlSeconds(long value) {
		staffTtlSeconds = value;
	}

	public long getStaffPersistentTtlSeconds() {
		return staffPersistentTtlSeconds;
	}

	public void setStaffPersistentTtlSeconds(long value) {
		staffPersistentTtlSeconds = value;
	}

	public long getPartnerTtlSeconds() {
		return partnerTtlSeconds;
	}

	public void setPartnerTtlSeconds(long value) {
		partnerTtlSeconds = value;
	}

	public long getPartnerPersistentTtlSeconds() {
		return partnerPersistentTtlSeconds;
	}

	public void setPartnerPersistentTtlSeconds(long value) {
		partnerPersistentTtlSeconds = value;
	}

	public long getUserTtlSeconds() {
		return userTtlSeconds;
	}

	public void setUserTtlSeconds(long value) {
		userTtlSeconds = value;
	}

	public long getUserPersistentTtlSeconds() {
		return userPersistentTtlSeconds;
	}

	public void setUserPersistentTtlSeconds(long value) {
		userPersistentTtlSeconds = value;
	}

	public String cleanupCron() {
		return cleanupCron;
	}

	public void setCleanupCron(String cleanupCron) {
		this.cleanupCron = cleanupCron;
	}
}
