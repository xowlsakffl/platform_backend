package com.medi.application.auth.command;

public record PasswordResetLinkCommand(
	String email,
	AuthClientContext client
) {
}
