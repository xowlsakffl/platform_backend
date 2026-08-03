package com.platform.application.specialist.query;

import com.platform.domain.specialist.SpecialistAllowStatus;
import com.platform.domain.specialist.SpecialistStatus;
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
