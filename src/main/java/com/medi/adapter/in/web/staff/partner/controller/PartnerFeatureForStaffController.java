package com.medi.adapter.in.web.staff.partner.controller;

import com.medi.adapter.in.web.staff.partner.request.PartnerFeatureListForStaffRequest;
import com.medi.application.partner.PartnerFeatureForStaffService;
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
@RequestMapping("/api/v1/staff/partner-features")
public class PartnerFeatureForStaffController {

	private final PartnerFeatureForStaffService service;

	public PartnerFeatureForStaffController(PartnerFeatureForStaffService service) {
		this.service = service;
	}

	@GetMapping
	public ApiResponse list(
		@AuthenticationPrincipal AuthenticatedActor actor,
		@Valid @ModelAttribute PartnerFeatureListForStaffRequest query,
		HttpServletRequest request
	) {
		return ApiResponse.success(service.list(actor, query.toQuery()), RequestTrace.traceId(request));
	}
}
