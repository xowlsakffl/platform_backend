package com.platform.application.auth.command;

public record AuthClientContext(
	String ipAddress,
	String userAgent
) {
	public AuthClientContext {
		ipAddress = trimToLength(ipAddress, 45);
		userAgent = trimToLength(userAgent, 500);
	}

	private static String trimToLength(String value, int maxLength) {
		if (value == null || value.isBlank()) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
	}
}
