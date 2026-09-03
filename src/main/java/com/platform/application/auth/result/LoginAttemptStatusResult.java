package com.platform.application.auth.result;

import java.time.LocalDateTime;

public record LoginAttemptStatusResult(
	int failureCount,
	boolean locked,
	LocalDateTime lockedUntil
) {
	public static LoginAttemptStatusResult unlocked() {
		return new LoginAttemptStatusResult(0, false, null);
	}
}
