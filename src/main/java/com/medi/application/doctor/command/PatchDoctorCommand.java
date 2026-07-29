package com.medi.application.doctor.command;

import com.medi.domain.doctor.DoctorAllowStatus;
import com.medi.domain.doctor.DoctorStatus;

public record PatchDoctorCommand(
	DoctorStatus status,
	DoctorAllowStatus allowStatus,
	String reason
) {
}
