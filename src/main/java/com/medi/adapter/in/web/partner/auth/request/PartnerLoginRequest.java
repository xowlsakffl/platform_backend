package com.medi.adapter.in.web.partner.auth.request;

import com.medi.application.auth.command.AuthLoginCommand;
import com.medi.application.auth.command.AuthClientContext;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PartnerLoginRequest(
	@NotBlank @Email @Size(max = 255) String email,
	@NotBlank @Size(max = 72) String password,
	@JsonProperty("keep_logged_in") boolean keepLoggedIn
) {

	public AuthLoginCommand toCommand(AuthClientContext client) {
		return new AuthLoginCommand(email, password, keepLoggedIn, client);
	}
}
