package com.platform.adapter.in.web.partner.invitation.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.platform.application.partner.command.AcceptPartnerAccountInvitationCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PartnerAccountInvitationAcceptRequest(
	@NotBlank @Size(min = 32, max = 200) String token,
	@JsonProperty("login_id") @NotBlank @Size(min = 4, max = 30)
	@Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9._-]{3,29}$") String loginId,
	@Pattern(regexp = "^(?:|[0-9+()\\-\\s]{7,50})$") String phone,
	@NotBlank @Size(min = 8, max = 72) String password
) {

	public AcceptPartnerAccountInvitationCommand toCommand() {
		return new AcceptPartnerAccountInvitationCommand(token, loginId, phone, password);
	}
}
