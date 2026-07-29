package com.medi.adapter.in.web.staff.hospital.request;

import com.medi.application.hospital.query.SearchHospitalFeaturesForStaffQuery;
import com.medi.domain.hospital.HospitalFeatureStatus;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

public record HospitalFeatureListForStaffRequest(
	@Size(max = 100) String q,
	List<HospitalFeatureStatus> status,
	@Pattern(regexp = "id|code|name|sort_order|status") String sort,
	@Pattern(regexp = "asc|desc") String direction
) {

	public SearchHospitalFeaturesForStaffQuery toQuery() {
		return new SearchHospitalFeaturesForStaffQuery(
			q,
			status == null || status.isEmpty() ? List.of(HospitalFeatureStatus.ACTIVE) : List.copyOf(status),
			sort == null || sort.isBlank() ? "sort_order" : sort,
			direction == null || direction.isBlank() ? "asc" : direction
		);
	}
}
