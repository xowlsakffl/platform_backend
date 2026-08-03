package com.platform.application.auth.command;

public record PasswordResetCommand(
	String email,
	String token,
	String password,
	AuthClientContext client
) {
}
