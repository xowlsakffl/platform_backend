package com.platform.application.auth.result;

public record IssuedPasswordResetToken(
	String rawToken,
	String tokenHash
) {
}
