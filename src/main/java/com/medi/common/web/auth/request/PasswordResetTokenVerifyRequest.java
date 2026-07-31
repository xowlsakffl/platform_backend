package com.medi.common.web.auth.request;

import com.medi.application.auth.command.AuthClientContext;
import com.medi.application.auth.command.PasswordResetTokenVerifyCommand;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordResetTokenVerifyRequest(
	@NotBlank @Email @Size(max = 255) String email,
	@NotBlank @Size(max = 255) String token
) {
	public PasswordResetTokenVerifyCommand toCommand(AuthClientContext client) {
		return new PasswordResetTokenVerifyCommand(email, token, client);
	}
}
