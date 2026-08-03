package com.platform.application.specialist.query;

import com.platform.domain.specialist.SpecialistAllowStatus;
import com.platform.domain.specialist.SpecialistField;
import java.util.Set;

public record SearchSpecialistsForStaffQuery(
	Long partnerId,
	String q,
	Set<SpecialistAllowStatus> allowStatus,
	Set<String> positions,
	Set<SpecialistField> specialistFields,
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
