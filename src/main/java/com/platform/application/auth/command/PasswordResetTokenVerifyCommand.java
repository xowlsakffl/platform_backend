package com.platform.application.auth.command;

public record PasswordResetTokenVerifyCommand(
	String email,
	String token,
	AuthClientContext client
) {
}
