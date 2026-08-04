package com.platform.adapter.in.web.staff.partner.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.platform.application.partner.command.CreatePartnerAccountInvitationCommand;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PartnerAccountInvitationCreateForStaffRequest(
	@NotBlank @Email @Size(max = 255) String email,
	@JsonProperty("recipient_name") @Size(max = 255) String recipientName
) {

	public CreatePartnerAccountInvitationCommand toCommand() {
		return new CreatePartnerAccountInvitationCommand(email, recipientName);
	}
}
