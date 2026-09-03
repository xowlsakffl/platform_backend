package com.platform.adapter.in.web.staff.account.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.BindParam;

public record PartnerAccountHistoryForStaffRequest(
	@Min(1) Integer page,
	@BindParam("per_page") @Min(1) @Max(50) Integer perPage
) {
	public int pageValue() {
		return page == null ? 1 : page;
	}

	public int perPageValue() {
		return perPage == null ? 10 : perPage;
	}
}
