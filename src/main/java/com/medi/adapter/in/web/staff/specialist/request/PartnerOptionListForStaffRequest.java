package com.medi.adapter.in.web.staff.specialist.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.BindParam;

public record PartnerOptionListForStaffRequest(
	@Size(max = 100) String q,
	@BindParam("per_page") @Min(1) @Max(20) Integer perPage
) {

	public int limit() {
		return perPage == null ? 10 : perPage;
	}
}
