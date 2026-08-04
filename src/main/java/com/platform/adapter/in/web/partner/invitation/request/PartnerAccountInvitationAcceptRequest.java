package com.platform.adapter.in.web.partner.invitation.request;

import com.platform.application.partner.command.AcceptPartnerAccountInvitationCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PartnerAccountInvitationAcceptRequest(
	@NotBlank @Size(min = 32, max = 200) String token,
	@NotBlank @Size(max = 255) String name,
	@NotBlank @Size(max = 255) String nickname,
	@Pattern(regexp = "^(?:|[0-9+()\\-\\s]{7,50})$") String phone,
	@NotBlank @Size(min = 8, max = 72) String password
) {

	public AcceptPartnerAccountInvitationCommand toCommand() {
		return new AcceptPartnerAccountInvitationCommand(token, name, nickname, phone, password);
	}
}
