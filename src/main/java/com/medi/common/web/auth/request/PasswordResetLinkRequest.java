package com.medi.common.web.auth.request;

import com.medi.application.auth.command.AuthClientContext;
import com.medi.application.auth.command.PasswordResetLinkCommand;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordResetLinkRequest(
	@NotBlank @Email @Size(max = 255) String email
) {
	public PasswordResetLinkCommand toCommand(AuthClientContext client) {
		return new PasswordResetLinkCommand(email, client);
	}
}
