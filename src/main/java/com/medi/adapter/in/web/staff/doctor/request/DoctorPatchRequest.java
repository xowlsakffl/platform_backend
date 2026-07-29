package com.medi.adapter.in.web.staff.doctor.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.medi.application.doctor.command.PatchDoctorCommand;
import com.medi.domain.doctor.DoctorAllowStatus;
import com.medi.domain.doctor.DoctorStatus;
import jakarta.validation.constraints.Size;

public record DoctorPatchRequest(
	DoctorStatus status,
	@JsonProperty("allow_status") DoctorAllowStatus allowStatus,
	@Size(max = 500) String reason
) {

	public PatchDoctorCommand toCommand() {
		return new PatchDoctorCommand(status, allowStatus, reason);
	}
}
