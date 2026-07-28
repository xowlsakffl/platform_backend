package com.medi.application.hospital.command;

import com.medi.domain.hospital.HospitalStatus;

public record ChangeHospitalStatusCommand(
	HospitalStatus status,
	String reason
) {
}
