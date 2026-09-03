package com.platform.application.account.result;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.platform.application.auth.result.ActiveAuthSessionResult;
import java.time.LocalDateTime;
import java.util.List;

public record PartnerAccountSecurityForStaffResult(
	@JsonProperty("failure_count") int failureCount,
	boolean locked,
	@JsonProperty("locked_until") LocalDateTime lockedUntil,
	@JsonProperty("active_sessions") List<ActiveAuthSessionResult> activeSessions
) {
}
