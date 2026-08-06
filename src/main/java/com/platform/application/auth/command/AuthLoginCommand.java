package com.platform.application.auth.command;

public record AuthLoginCommand(
	String identifier,
	String password,
	boolean keepLoggedIn,
	AuthClientContext client
) {
}
