package com.medi.application.specialist.command;

import com.medi.domain.specialist.SpecialistAllowStatus;
import com.medi.domain.specialist.SpecialistStatus;

public record UpdateSpecialistStatusForStaffCommand(
	SpecialistStatus status,
	SpecialistAllowStatus allowStatus,
	String reason
) {
}
