package com.platform.application.partner.query;

import com.platform.domain.partner.PartnerFeatureStatus;
import java.util.List;

public record SearchPartnerFeaturesForStaffQuery(
	String q,
	List<PartnerFeatureStatus> status,
	String sort,
	String direction
) {
}
