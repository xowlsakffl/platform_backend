package com.platform.application.notice.result;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.List;

public record NoticeHistoryResult(Long id, String action,
	@JsonProperty("target_type") String targetType,
	@JsonProperty("target_id") Long targetId,
	@JsonProperty("actor_type") String actorType,
	@JsonProperty("actor_id") Long actorId,
	@JsonProperty("actor_name") String actorName,
	@JsonProperty("actor_login_id") String actorLoginId,
	@JsonProperty("ip_address") String ipAddress,
	@JsonProperty("created_at") LocalDateTime createdAt,
	List<Change> changes) {
	public record Change(@JsonProperty("field_key") String fieldKey,
		@JsonProperty("before_value") String beforeValue, @JsonProperty("after_value") String afterValue) {}
}
