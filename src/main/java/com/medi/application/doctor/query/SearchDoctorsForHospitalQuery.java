package com.medi.application.doctor.query;

import com.medi.domain.doctor.DoctorAllowStatus;
import com.medi.domain.doctor.DoctorStatus;
import java.util.Set;

public record SearchDoctorsForHospitalQuery(
	String q,
	Set<DoctorStatus> statuses,
	Set<DoctorAllowStatus> allowStatuses,
	String sort,
	String direction,
	int page,
	int perPage
) {
}
