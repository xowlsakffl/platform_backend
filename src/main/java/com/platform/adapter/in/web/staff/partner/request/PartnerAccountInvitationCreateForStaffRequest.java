package com.platform.adapter.in.web.staff.partner.request;

import com.platform.application.partner.command.CreatePartnerAccountInvitationCommand;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PartnerAccountInvitationCreateForStaffRequest(
	@NotBlank @Email @Size(max = 255) String email
) {

	public CreatePartnerAccountInvitationCommand toCommand() {
		return new CreatePartnerAccountInvitationCommand(email);
	}
}
