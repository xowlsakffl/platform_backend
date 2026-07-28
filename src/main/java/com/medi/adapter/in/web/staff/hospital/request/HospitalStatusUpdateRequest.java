package com.medi.adapter.in.web.staff.hospital.request;

import com.medi.application.hospital.command.ChangeHospitalStatusCommand;
import com.medi.domain.hospital.HospitalStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record HospitalStatusUpdateRequest(
	@NotNull HospitalStatus status,
	@Size(max = 500) String reason
) {

	public ChangeHospitalStatusCommand toCommand() {
		return new ChangeHospitalStatusCommand(status, reason);
	}
}
