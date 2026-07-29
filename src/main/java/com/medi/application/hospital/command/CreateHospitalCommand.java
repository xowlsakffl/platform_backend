package com.medi.application.hospital.command;

import com.medi.application.media.storage.MediaFileSource;
import com.medi.domain.hospital.HospitalAllowStatus;
import com.medi.domain.hospital.HospitalInterpretationLanguage;
import com.medi.domain.hospital.HospitalStatus;
import java.util.Set;
import java.util.List;

public record CreateHospitalCommand(
	String name,
	String description,
	String youtubeLink,
	String address,
	String addressDetail,
	String latitude,
	String longitude,
	String consultingHours,
	Object operationHours,
	String direction,
	HospitalAllowStatus allowStatus,
	HospitalStatus status,
	HospitalContactSetCommand contacts,
	HospitalBusinessRegistrationCommand businessRegistration,
	Set<Long> categoryIds,
	Set<Long> featureIds,
	Set<HospitalInterpretationLanguage> interpretationLanguages,
	MediaFileSource logo,
	List<MediaFileSource> gallery,
	MediaFileSource businessRegistrationFile
) {
}
