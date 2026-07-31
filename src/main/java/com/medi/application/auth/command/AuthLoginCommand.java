package com.medi.application.auth.command;

public record AuthLoginCommand(
	String email,
	String password,
	boolean keepLoggedIn,
	AuthClientContext client
) {
}
