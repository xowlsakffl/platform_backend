package com.platform.adapter.in.web.partner.onboarding.controller;

import com.platform.adapter.in.web.partner.onboarding.request.PartnerOnboardingUpdateRequest;
import com.platform.application.partner.PartnerOnboardingService;
import com.platform.common.security.AuthenticatedActor;
import com.platform.common.web.ApiResponse;
import com.platform.common.web.RequestTrace;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/partner/partners/{partnerId}/onboarding")
public class PartnerOnboardingController {

	private final PartnerOnboardingService service;

	public PartnerOnboardingController(PartnerOnboardingService service) {
		this.service = service;
	}

	@GetMapping
	public ApiResponse get(
		@AuthenticationPrincipal AuthenticatedActor actor,
		@PathVariable Long partnerId,
		HttpServletRequest request
	) {
		return ApiResponse.success(service.get(actor, partnerId), RequestTrace.traceId(request));
	}

	@GetMapping("/features")
	public ApiResponse features(
		@AuthenticationPrincipal AuthenticatedActor actor,
		@PathVariable Long partnerId,
		HttpServletRequest request
	) {
		return ApiResponse.success(
			service.availableFeatures(actor, partnerId),
			RequestTrace.traceId(request)
		);
	}

	@PatchMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ApiResponse update(
		@AuthenticationPrincipal AuthenticatedActor actor,
		@PathVariable Long partnerId,
		@Valid @ModelAttribute PartnerOnboardingUpdateRequest body,
		HttpServletRequest request
	) {
		return ApiResponse.success(
			service.update(actor, partnerId, body.toCommand(request.getParameterMap().keySet())),
			RequestTrace.traceId(request)
		);
	}

	@PostMapping("/submit")
	public ApiResponse submit(
		@AuthenticationPrincipal AuthenticatedActor actor,
		@PathVariable Long partnerId,
		HttpServletRequest request
	) {
		return ApiResponse.success(service.submit(actor, partnerId), RequestTrace.traceId(request));
	}
}
