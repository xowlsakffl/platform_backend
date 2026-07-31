package com.medi.adapter.in.web.staff.partner.request;

import com.medi.application.partner.query.SearchPartnerFeaturesForStaffQuery;
import com.medi.domain.partner.PartnerFeatureStatus;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

public record PartnerFeatureListForStaffRequest(
	@Size(max = 100) String q,
	List<PartnerFeatureStatus> status,
	@Pattern(regexp = "id|code|name|sort_order|status") String sort,
	@Pattern(regexp = "asc|desc") String direction
) {

	public SearchPartnerFeaturesForStaffQuery toQuery() {
		return new SearchPartnerFeaturesForStaffQuery(
			q,
			status == null || status.isEmpty() ? List.of(PartnerFeatureStatus.ACTIVE) : List.copyOf(status),
			sort == null || sort.isBlank() ? "sort_order" : sort,
			direction == null || direction.isBlank() ? "asc" : direction
		);
	}
}
