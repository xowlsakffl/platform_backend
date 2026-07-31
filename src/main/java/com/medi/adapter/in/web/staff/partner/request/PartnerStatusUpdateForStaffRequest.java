package com.medi.adapter.in.web.staff.partner.request;

import com.medi.application.partner.command.ChangePartnerStatusCommand;
import com.medi.domain.partner.PartnerStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PartnerStatusUpdateForStaffRequest(
	@NotNull PartnerStatus status,
	@Size(max = 500) String reason
) {

	public ChangePartnerStatusCommand toCommand() {
		return new ChangePartnerStatusCommand(status, reason);
	}
}
