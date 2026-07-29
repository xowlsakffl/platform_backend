package com.medi.adapter.in.web.staff.hospital.controller;

import com.medi.adapter.in.web.staff.hospital.request.HospitalFeatureListForStaffRequest;
import com.medi.application.hospital.HospitalFeatureForStaffService;
import com.medi.common.security.AuthenticatedActor;
import com.medi.common.web.ApiResponse;
import com.medi.common.web.RequestTrace;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/staff/hospital-features")
public class HospitalFeatureForStaffController {

	private final HospitalFeatureForStaffService service;

	public HospitalFeatureForStaffController(HospitalFeatureForStaffService service) {
		this.service = service;
	}

	@GetMapping
	public ApiResponse list(
		@AuthenticationPrincipal AuthenticatedActor actor,
		@Valid @ModelAttribute HospitalFeatureListForStaffRequest query,
		HttpServletRequest request
	) {
		return ApiResponse.success(service.list(actor, query.toQuery()), RequestTrace.traceId(request));
	}
}
