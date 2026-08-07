package com.platform.adapter.in.web.staff.partner.controller;

import com.platform.adapter.in.web.staff.partner.request.PartnerOptionCreateForStaffRequest;
import com.platform.adapter.in.web.staff.partner.request.PartnerOptionReplaceForStaffRequest;
import com.platform.application.partner.PartnerOptionForPartnerService;
import com.platform.common.security.AuthenticatedActor;
import com.platform.common.web.ApiResponse;
import com.platform.common.web.RequestTrace;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/staff/partners/{partnerId}/options")
public class PartnerOptionForStaffController {

	private final PartnerOptionForPartnerService service;

	public PartnerOptionForStaffController(PartnerOptionForPartnerService service) {
		this.service = service;
	}

	@GetMapping
	public ApiResponse list(
		@AuthenticationPrincipal AuthenticatedActor actor,
		@PathVariable Long partnerId,
		HttpServletRequest request
	) {
		return ApiResponse.success(service.listForStaff(actor, partnerId), RequestTrace.traceId(request));
	}

	@PutMapping
	public ApiResponse replace(
		@AuthenticationPrincipal AuthenticatedActor actor,
		@PathVariable Long partnerId,
		@Valid @RequestBody PartnerOptionReplaceForStaffRequest body,
		HttpServletRequest request
	) {
		return ApiResponse.success(
			service.replaceForStaff(actor, partnerId, body.toCommand()),
			RequestTrace.traceId(request)
		);
	}

	@PostMapping
	public ApiResponse create(
		@AuthenticationPrincipal AuthenticatedActor actor,
		@PathVariable Long partnerId,
		@Valid @RequestBody PartnerOptionCreateForStaffRequest body,
		HttpServletRequest request
	) {
		return ApiResponse.success(
			service.createForStaff(actor, partnerId, body.toCommand()),
			RequestTrace.traceId(request)
		);
	}

	@PatchMapping("/{optionId}")
	public ApiResponse update(
		@AuthenticationPrincipal AuthenticatedActor actor,
		@PathVariable Long partnerId,
		@PathVariable Long optionId,
		@Valid @RequestBody PartnerOptionCreateForStaffRequest body,
		HttpServletRequest request
	) {
		return ApiResponse.success(
			service.updateForStaff(actor, partnerId, optionId, body.toCommand()),
			RequestTrace.traceId(request)
		);
	}

	@DeleteMapping("/{optionId}")
	public ApiResponse delete(
		@AuthenticationPrincipal AuthenticatedActor actor,
		@PathVariable Long partnerId,
		@PathVariable Long optionId,
		HttpServletRequest request
	) {
		return ApiResponse.success(
			service.deleteForStaff(actor, partnerId, optionId),
			RequestTrace.traceId(request)
		);
	}
}
