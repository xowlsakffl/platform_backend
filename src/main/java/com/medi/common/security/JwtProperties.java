package com.medi.common.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.auth.jwt")
public class JwtProperties {

	private String issuer = "medi_backend";
	private String secret = "local-medi-auth-secret-change-before-production";
	private long accessTokenTtlSeconds = 7200;

	public String issuer() {
		return issuer;
	}

	public void setIssuer(String issuer) {
		this.issuer = issuer;
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
