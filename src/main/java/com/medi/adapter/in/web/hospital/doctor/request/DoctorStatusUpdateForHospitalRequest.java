package com.medi.adapter.in.web.hospital.doctor.request;

import com.medi.domain.doctor.DoctorStatus;
import jakarta.validation.constraints.NotNull;

public record DoctorStatusUpdateForHospitalRequest(
	@NotNull DoctorStatus status
) {
}
