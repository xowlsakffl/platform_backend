package com.medi.adapter.in.web.staff.doctor.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.medi.application.doctor.command.UpdateDoctorStatusForStaffCommand;
import com.medi.domain.doctor.DoctorAllowStatus;
import com.medi.domain.doctor.DoctorStatus;
import jakarta.validation.constraints.Size;

public record DoctorStatusUpdateForStaffRequest(
	DoctorStatus status,
	@JsonProperty("allow_status") DoctorAllowStatus allowStatus,
	@Size(max = 500) String reason
) {

	public UpdateDoctorStatusForStaffCommand toCommand() {
		return new UpdateDoctorStatusForStaffCommand(status, allowStatus, reason);
	}
}
