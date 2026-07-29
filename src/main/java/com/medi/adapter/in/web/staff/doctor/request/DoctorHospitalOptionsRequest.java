package com.medi.adapter.in.web.staff.doctor.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.BindParam;

public record DoctorHospitalOptionsRequest(
	@Size(max = 100) String q,
	@BindParam("per_page") @Min(1) @Max(50) Integer perPage
) {

	public int limit() {
		return perPage == null ? 10 : perPage;
	}
}
