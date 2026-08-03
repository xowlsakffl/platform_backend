package com.platform.application.partner.result;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.List;

public record OperationHistoryResult(
	Long id,
	String action,
	String reason,
	@JsonProperty("created_at") LocalDateTime createdAt,
	List<OperationHistoryChangeResult> changes
) {
}
