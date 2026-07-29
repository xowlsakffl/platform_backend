package com.medi.adapter.in.web.staff.auth.request;

import com.medi.application.auth.command.AuthLoginCommand;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record StaffLoginRequest(
	@NotBlank @Email String email,
	@NotBlank String password
) {

	public AuthLoginCommand toCommand() {
		return new AuthLoginCommand(email, password);
	}
}
