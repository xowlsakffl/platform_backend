package com.platform.adapter.in.web.staff.auth.request;

import com.platform.application.auth.command.AuthLoginCommand;
import com.platform.application.auth.command.AuthClientContext;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record StaffLoginRequest(
	@JsonProperty("login_id") @NotBlank @Size(min = 4, max = 30)
	@Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9._-]{3,29}$") String loginId,
	@NotBlank @Size(max = 72) String password,
	@JsonProperty("keep_logged_in") boolean keepLoggedIn
) {

	public AuthLoginCommand toCommand(AuthClientContext client) {
		return new AuthLoginCommand(loginId, password, keepLoggedIn, client);
	}
}
