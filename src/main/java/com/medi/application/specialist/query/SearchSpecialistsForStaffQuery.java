package com.medi.application.specialist.query;

import com.medi.domain.specialist.SpecialistAllowStatus;
import com.medi.domain.specialist.SpecialistField;
import java.util.Set;

public record SearchSpecialistsForStaffQuery(
	Long partnerId,
	String q,
	Set<SpecialistAllowStatus> allowStatus,
	Set<String> positions,
	Set<SpecialistField> specialistFields,
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
