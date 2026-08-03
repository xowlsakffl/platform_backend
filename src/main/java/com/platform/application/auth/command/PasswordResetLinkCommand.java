package com.platform.application.auth.command;

public record PasswordResetLinkCommand(
	String email,
	AuthClientContext client
) {
}
