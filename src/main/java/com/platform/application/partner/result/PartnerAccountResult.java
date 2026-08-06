package com.platform.application.partner.result;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

public record PartnerAccountResult(
	Long id,
	@JsonProperty("login_id") String loginId,
	String email,
	String phone,
	String status,
	@JsonProperty("last_login_at") LocalDateTime lastLoginAt,
	@JsonProperty("created_at") LocalDateTime createdAt,
	@JsonProperty("updated_at") LocalDateTime updatedAt
) {
}
