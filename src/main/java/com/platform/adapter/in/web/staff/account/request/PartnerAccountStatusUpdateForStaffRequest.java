package com.platform.adapter.in.web.staff.account.request;

import com.platform.application.partner.command.ChangePartnerAccountStatusCommand;
import com.platform.domain.account.AccountPartnerStatus;
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
