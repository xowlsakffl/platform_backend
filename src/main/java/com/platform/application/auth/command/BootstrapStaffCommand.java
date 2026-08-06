package com.platform.application.auth.command;

public record BootstrapStaffCommand(
	String loginId,
	String email,
	String password,
	String name,
	String nickname,
	String roleName
) {
}
