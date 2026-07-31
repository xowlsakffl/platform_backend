package com.medi.application.auth.result;

public record IssuedPasswordResetToken(
	String rawToken,
	String tokenHash
) {
}
