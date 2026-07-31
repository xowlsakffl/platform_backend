package com.medi.application.partner.query;

import com.medi.domain.partner.PartnerFeatureStatus;
import java.util.List;

public record SearchPartnerFeaturesForStaffQuery(
	String q,
	List<PartnerFeatureStatus> status,
	String sort,
	String direction
) {
}
