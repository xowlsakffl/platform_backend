package com.platform.adapter.in.web.partner.option.controller;

import com.platform.adapter.in.web.partner.option.request.PartnerOptionSaveRequest;
import com.platform.application.partner.PartnerOptionForPartnerService;
import com.platform.common.security.AuthenticatedActor;
import com.platform.common.web.ApiResponse;
import com.platform.common.web.RequestTrace;
import com.platform.common.web.partner.PartnerOptionsReplaceRequest;
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
@RequestMapping("/api/v1/partner/partners/{partnerId}/options")
public class PartnerOptionForPartnerController {

	private final PartnerOptionForPartnerService service;

	public PartnerOptionForPartnerController(PartnerOptionForPartnerService service) {
		this.service = service;
	}

	@GetMapping
	public ApiResponse list(
		@AuthenticationPrincipal AuthenticatedActor actor,
		@PathVariable Long partnerId,
		HttpServletRequest request
	) {
		return ApiResponse.success(service.list(actor, partnerId), RequestTrace.traceId(request));
	}

	@PutMapping
	public ApiResponse replace(
		@AuthenticationPrincipal AuthenticatedActor actor,
		@PathVariable Long partnerId,
		@Valid @RequestBody PartnerOptionsReplaceRequest body,
		HttpServletRequest request
	) {
		return ApiResponse.success(service.replaceForPartner(actor, partnerId, body.toCommand()),
			RequestTrace.traceId(request));
	}

	@PostMapping
	public ApiResponse create(
		@AuthenticationPrincipal AuthenticatedActor actor,
		@PathVariable Long partnerId,
		@Valid @RequestBody PartnerOptionSaveRequest body,
		HttpServletRequest request
	) {
		return ApiResponse.success(service.create(actor, partnerId, body.toCommand()), RequestTrace.traceId(request));
	}

	@PatchMapping("/{id}")
	public ApiResponse update(
		@AuthenticationPrincipal AuthenticatedActor actor,
		@PathVariable Long partnerId,
		@PathVariable Long id,
		@Valid @RequestBody PartnerOptionSaveRequest body,
		HttpServletRequest request
	) {
		return ApiResponse.success(service.update(actor, partnerId, id, body.toCommand()), RequestTrace.traceId(request));
	}

	@DeleteMapping("/{id}")
	public ApiResponse delete(
		@AuthenticationPrincipal AuthenticatedActor actor,
		@PathVariable Long partnerId,
		@PathVariable Long id,
		HttpServletRequest request
	) {
		return ApiResponse.success(service.delete(actor, partnerId, id), RequestTrace.traceId(request));
	}
}
