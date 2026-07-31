package com.medi.common.error;

import java.util.Map;

public class RateLimitException extends ApiException {

	private final long retryAfterSeconds;

	public RateLimitException(String message, long retryAfterSeconds) {
		super(
			ErrorCode.RATE_LIMITED,
			message + " " + formatDuration(retryAfterSeconds) + " 후 다시 시도해주세요.",
			Map.of("retry_after_seconds", Math.max(1, retryAfterSeconds))
		);
		this.retryAfterSeconds = Math.max(1, retryAfterSeconds);
	}

	public long retryAfterSeconds() {
		return retryAfterSeconds;
	}

	private static String formatDuration(long value) {
		long seconds = Math.max(1, value);
		long minutes = seconds / 60;
		long remainingSeconds = seconds % 60;
		if (minutes == 0) {
			return seconds + "초";
		}
		if (remainingSeconds == 0) {
			return minutes + "분";
		}
		return minutes + "분 " + remainingSeconds + "초";
	}
}
