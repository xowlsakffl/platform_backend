package com.medi.adapter.in.web.staff.partner.request;

import com.medi.application.partner.query.GetPartnerForStaffQuery;
import com.medi.application.partner.query.SearchPartnerOperationHistoriesForStaffQuery;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Set;
import org.springframework.web.bind.annotation.BindParam;

public record PartnerGetForStaffRequest(
	@Size(max = 5)
	List<@Pattern(regexp = "business_registration|account_partner|account_partners|specialists|categories|features") String> include,
	@BindParam("operation_histories_page") @Min(1) Integer operationHistoriesPage,
	@BindParam("operation_histories_per_page") @Min(1) @Max(50) Integer operationHistoriesPerPage
) {

	public GetPartnerForStaffQuery toQuery() {
		return new GetPartnerForStaffQuery(include == null
			? Set.of()
			: include.stream()
				.map(value -> "account_partners".equals(value) ? "account_partner" : value)
				.collect(java.util.stream.Collectors.toUnmodifiableSet()));
	}

	public SearchPartnerOperationHistoriesForStaffQuery toHistoryQuery() {
		return new SearchPartnerOperationHistoriesForStaffQuery(
			operationHistoriesPage == null ? 1 : operationHistoriesPage,
			operationHistoriesPerPage == null ? 10 : operationHistoriesPerPage
		);
	}
}
