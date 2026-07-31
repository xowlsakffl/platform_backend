package com.medi.application.partner.result;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

public record PartnerAccountResult(
	Long id,
	String name,
	String nickname,
	String email,
	String phone,
	String status,
	@JsonProperty("last_login_at") LocalDateTime lastLoginAt,
	@JsonProperty("created_at") LocalDateTime createdAt,
	@JsonProperty("updated_at") LocalDateTime updatedAt
) {
}
