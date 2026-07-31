package com.medi.adapter.in.web.staff.partner.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.medi.application.partner.command.ChangePartnerAllowStatusCommand;
import com.medi.domain.partner.PartnerAllowStatus;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record PartnerAllowStatusUpdateForStaffRequest(
	@NotEmpty List<Long> ids,
	@JsonProperty("allow_status") @NotNull PartnerAllowStatus allowStatus,
	@Size(max = 500) String reason
) {

	public ChangePartnerAllowStatusCommand toCommand() {
		return new ChangePartnerAllowStatusCommand(ids, allowStatus, reason);
	}
}
