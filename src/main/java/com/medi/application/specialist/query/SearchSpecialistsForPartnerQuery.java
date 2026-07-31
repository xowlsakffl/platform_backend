package com.medi.application.specialist.query;

import com.medi.domain.specialist.SpecialistAllowStatus;
import com.medi.domain.specialist.SpecialistStatus;
import java.util.Set;

public record SearchSpecialistsForPartnerQuery(
	String q,
	Set<SpecialistStatus> statuses,
	Set<SpecialistAllowStatus> allowStatuses,
	String sort,
	String direction,
	int page,
	int perPage
) {
}
