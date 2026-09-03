package com.platform.application.partner.result;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

public record OwnedPartnerResult(
	Long id,
	String name,
	@JsonProperty("allow_status") String allowStatus,
	String status,
	String role,
	@JsonProperty("created_at") LocalDateTime createdAt
) {
}
