package com.medi.adapter.in.web.staff.hospital.request;

import com.medi.application.hospital.query.GetHospitalForStaffQuery;
import com.medi.application.hospital.query.SearchHospitalOperationHistoriesForStaffQuery;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Set;
import org.springframework.web.bind.annotation.BindParam;

public record HospitalGetForStaffRequest(
	@Size(max = 5)
	List<@Pattern(regexp = "business_registration|account_hospital|account_hospitals|doctors|categories|features") String> include,
	@BindParam("operation_histories_page") @Min(1) Integer operationHistoriesPage,
	@BindParam("operation_histories_per_page") @Min(1) @Max(50) Integer operationHistoriesPerPage
) {

	public GetHospitalForStaffQuery toQuery() {
		return new GetHospitalForStaffQuery(include == null
			? Set.of()
			: include.stream()
				.map(value -> "account_hospitals".equals(value) ? "account_hospital" : value)
				.collect(java.util.stream.Collectors.toUnmodifiableSet()));
	}

	public SearchHospitalOperationHistoriesForStaffQuery toHistoryQuery() {
		return new SearchHospitalOperationHistoriesForStaffQuery(
			operationHistoriesPage == null ? 1 : operationHistoriesPage,
			operationHistoriesPerPage == null ? 10 : operationHistoriesPerPage
		);
	}
}
