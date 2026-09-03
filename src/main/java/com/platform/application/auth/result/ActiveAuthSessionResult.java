package com.platform.application.auth.result;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

public record ActiveAuthSessionResult(
	String id,
	boolean persistent,
	@JsonProperty("created_at") LocalDateTime createdAt,
	@JsonProperty("last_used_at") LocalDateTime lastUsedAt,
	@JsonProperty("expires_at") LocalDateTime expiresAt,
	@JsonProperty("ip_address") String ipAddress,
	@JsonProperty("user_agent") String userAgent
) {
}
