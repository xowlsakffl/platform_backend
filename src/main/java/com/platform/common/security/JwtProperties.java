package com.platform.common.security;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "app.auth.jwt")
@Validated
public class JwtProperties {

	@NotBlank
	private String issuer = "platform_backend";

	@NotBlank
	private String audience = "platform-api";

	@NotBlank
	@Size(min = 32)
	private String secret;

	@Min(300)
	@Max(3600)
	private long accessTokenTtlSeconds = 900;

	public String issuer() {
		return issuer;
	}

	public void setIssuer(String issuer) {
		this.issuer = issuer;
	}

	public String audience() {
		return audience;
	}

	public void setAudience(String audience) {
		this.audience = audience;
	}

	public String secret() {
		return secret;
	}

	public void setSecret(String secret) {
		this.secret = secret;
	}

	public long accessTokenTtlSeconds() {
		return accessTokenTtlSeconds;
	}

	public void setAccessTokenTtlSeconds(long accessTokenTtlSeconds) {
		this.accessTokenTtlSeconds = accessTokenTtlSeconds;
	}
}
