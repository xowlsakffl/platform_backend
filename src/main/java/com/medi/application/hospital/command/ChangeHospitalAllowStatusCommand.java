package com.medi.application.hospital.command;

import com.medi.domain.hospital.HospitalAllowStatus;
import java.util.List;

public record ChangeHospitalAllowStatusCommand(
	List<Long> ids,
	HospitalAllowStatus allowStatus,
	String reason
) {
}
