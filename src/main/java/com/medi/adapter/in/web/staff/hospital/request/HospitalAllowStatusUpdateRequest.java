package com.medi.adapter.in.web.staff.hospital.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.medi.application.hospital.command.ChangeHospitalAllowStatusCommand;
import com.medi.domain.hospital.HospitalAllowStatus;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record HospitalAllowStatusUpdateRequest(
	@NotEmpty List<Long> ids,
	@JsonProperty("allow_status") @NotNull HospitalAllowStatus allowStatus,
	@Size(max = 500) String reason
) {

	public ChangeHospitalAllowStatusCommand toCommand() {
		return new ChangeHospitalAllowStatusCommand(ids, allowStatus, reason);
	}
}
