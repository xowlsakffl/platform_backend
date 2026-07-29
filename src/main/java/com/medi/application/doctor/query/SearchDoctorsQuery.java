package com.medi.application.doctor.query;

import com.medi.domain.doctor.DoctorAllowStatus;
import com.medi.domain.doctor.DoctorSpecialistField;
import java.util.Set;

public record SearchDoctorsQuery(
	String q,
	Set<DoctorAllowStatus> allowStatus,
	Set<String> positions,
	Set<DoctorSpecialistField> specialistFields,
	Set<Long> categoryIds,
	String metric,
	Integer metricMin,
	Integer metricMax,
	String startDate,
	String endDate,
	String sort,
	String direction,
	int page,
	int perPage
) {
}
