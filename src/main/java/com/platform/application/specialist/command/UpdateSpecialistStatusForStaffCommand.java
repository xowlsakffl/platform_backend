package com.platform.application.specialist.command;

import com.platform.domain.specialist.SpecialistAllowStatus;
import com.platform.domain.specialist.SpecialistStatus;

public record UpdateSpecialistStatusForStaffCommand(
	SpecialistStatus status,
	SpecialistAllowStatus allowStatus,
	String reason
) {
}
