package com.platform.application.account.result;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

public record PartnerAccountAccessEventForStaffResult(
	Long id,
	String result,
	@JsonProperty("failure_code") String failureCode,
	@JsonProperty("ip_address") String ipAddress,
	@JsonProperty("user_agent") String userAgent,
	@JsonProperty("created_at") LocalDateTime createdAt
) {
}
