package com.platform.application.account.result;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.List;

public record PartnerAccountManagementHistoryForStaffResult(
	Long id,
	String action,
	String reason,
	String memo,
	@JsonProperty("target_type") String targetType,
	@JsonProperty("target_id") Long targetId,
	@JsonProperty("target_name") String targetName,
	@JsonProperty("actor_type") String actorType,
	@JsonProperty("actor_id") Long actorId,
	@JsonProperty("actor_name") String actorName,
	@JsonProperty("actor_login_id") String actorLoginId,
	@JsonProperty("ip_address") String ipAddress,
	@JsonProperty("user_agent") String userAgent,
	@JsonProperty("created_at") LocalDateTime createdAt,
	List<PartnerAccountManagementHistoryChangeForStaffResult> changes
) {
}
