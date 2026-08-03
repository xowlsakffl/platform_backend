package com.platform.common.web.auth.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.platform.application.auth.command.AuthClientContext;
import com.platform.application.auth.command.PasswordResetCommand;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordResetRequest(
	@NotBlank @Email @Size(max = 255) String email,
	@NotBlank @Size(max = 255) String token,
	@NotBlank @Size(min = 8, max = 72) String password,
	@JsonProperty("password_confirmation") @NotBlank @Size(min = 8, max = 72) String passwordConfirmation
) {
	@AssertTrue(message = "비밀번호 확인이 일치하지 않습니다.")
	public boolean isPasswordConfirmed() {
		return password != null && password.equals(passwordConfirmation);
	}

	public PasswordResetCommand toCommand(AuthClientContext client) {
		return new PasswordResetCommand(email, token, password, client);
	}
}
