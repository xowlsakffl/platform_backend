package com.platform.adapter.in.web.partner.auth.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.platform.application.auth.command.AuthClientContext;
import com.platform.application.auth.command.AuthLoginCommand;
import com.platform.application.auth.command.RegisterPartnerAccountCommand;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PartnerSignupRequest(
	@NotBlank @Size(max = 50) String name,
	@JsonProperty("login_id") @NotBlank @Size(min = 4, max = 30)
	@Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9._-]{3,29}$") String loginId,
	@NotBlank @Email @Size(max = 255) String email,
	@Pattern(regexp = "^(?:|[0-9+()\\-\\s]{7,50})$") String phone,
	@NotBlank @Size(min = 8, max = 72) String password,
	@JsonProperty("keep_logged_in") boolean keepLoggedIn
) {

	public RegisterPartnerAccountCommand toRegistrationCommand() {
		return new RegisterPartnerAccountCommand(name, loginId, email, phone, password);
	}

	public AuthLoginCommand toLoginCommand(AuthClientContext client) {
		return new AuthLoginCommand(loginId, password, keepLoggedIn, client);
	}
}
