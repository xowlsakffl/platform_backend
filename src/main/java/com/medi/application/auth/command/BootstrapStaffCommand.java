package com.medi.application.auth.command;

public record BootstrapStaffCommand(
	String email,
	String password,
	String name,
	String nickname,
	String roleName
) {
}
