package com.platform.application.auth.command;

public record RegisterPartnerAccountCommand(
	String name,
	String loginId,
	String email,
	String phone,
	String password
) {
}
