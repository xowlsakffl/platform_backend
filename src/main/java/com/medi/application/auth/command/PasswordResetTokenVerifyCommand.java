package com.medi.application.auth.command;

public record PasswordResetTokenVerifyCommand(
	String email,
	String token,
	AuthClientContext client
) {
}
