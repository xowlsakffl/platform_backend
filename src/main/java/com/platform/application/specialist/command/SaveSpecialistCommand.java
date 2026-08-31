package com.platform.application.specialist.command;

import com.platform.application.media.storage.MediaFileSource;
import com.platform.domain.specialist.SpecialistAllowStatus;
import com.platform.domain.specialist.SpecialistField;
import com.platform.domain.specialist.SpecialistScheduleMode;
import com.platform.domain.specialist.SpecialistStatus;
import java.time.LocalDate;
import java.util.List;

public record SaveSpecialistCommand(
	Long partnerId,
	String name,
	String gender,
	String position,
	LocalDate careerStartedAt,
	SpecialistField specialistField,
	String introduction,
	SpecialistScheduleMode scheduleMode,
	Object operationHours,
	Object holidayPolicy,
	SpecialistStatus status,
	SpecialistAllowStatus allowStatus,
	String optionAssignments,
	List<MediaFileSource> profileImages,
	List<String> profileImageOrder,
	List<MediaFileSource> certificationImages,
	List<String> certificationImageOrder
) {
}
