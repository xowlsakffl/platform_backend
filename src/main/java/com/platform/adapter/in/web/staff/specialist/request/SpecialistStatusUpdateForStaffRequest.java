package com.platform.adapter.in.web.staff.specialist.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.platform.application.specialist.command.UpdateSpecialistStatusForStaffCommand;
import com.platform.domain.specialist.SpecialistAllowStatus;
import com.platform.domain.specialist.SpecialistStatus;
import jakarta.validation.constraints.Size;

public record SpecialistStatusUpdateForStaffRequest(
	SpecialistStatus status,
	@JsonProperty("allow_status") SpecialistAllowStatus allowStatus,
	@Size(max = 500) String reason
) {

	public UpdateSpecialistStatusForStaffCommand toCommand() {
		return new UpdateSpecialistStatusForStaffCommand(status, allowStatus, reason);
	}
}
