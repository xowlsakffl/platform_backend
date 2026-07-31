package com.medi.adapter.in.web.staff.partner.request;

import com.medi.application.partner.command.ChangePartnerAccountStatusCommand;
import com.medi.domain.account.AccountPartnerStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PartnerAccountStatusUpdateForStaffRequest(
	@NotNull AccountPartnerStatus status,
	@Size(max = 500) String reason
) {

	public ChangePartnerAccountStatusCommand toCommand() {
		return new ChangePartnerAccountStatusCommand(status, reason);
	}
}
