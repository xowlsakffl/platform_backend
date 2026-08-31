package com.platform.application.specialist.command;

import com.platform.application.media.storage.MediaFileSource;
import com.platform.domain.specialist.SpecialistField;
import com.platform.domain.specialist.SpecialistScheduleMode;
import com.platform.domain.specialist.SpecialistStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

public record UpdateSpecialistForPartnerCommand(
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
	String optionAssignments,
	List<MediaFileSource> profileImages,
	List<String> profileImageOrder,
	List<MediaFileSource> certificationImages,
	List<String> certificationImageOrder,
	Set<String> specifiedFields
) {

	public boolean specified(String field) {
		return specifiedFields.contains(field);
	}
}
