package com.medi.application.hospital.command;

import com.medi.domain.hospital.HospitalAllowStatus;
import com.medi.domain.hospital.HospitalDepartment;
import com.medi.domain.hospital.HospitalStatus;
import java.util.Set;

public record UpdateHospitalCommand(
	HospitalDepartment department,
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
	Set<Long> featureIds
) {
}
