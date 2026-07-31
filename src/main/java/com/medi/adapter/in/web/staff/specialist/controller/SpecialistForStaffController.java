package com.medi.adapter.in.web.staff.specialist.controller;

import com.medi.adapter.in.web.staff.specialist.request.SpecialistCreateForStaffRequest;
import com.medi.adapter.in.web.staff.specialist.request.SpecialistListForStaffRequest;
import com.medi.adapter.in.web.staff.specialist.request.SpecialistStatusUpdateForStaffRequest;
import com.medi.adapter.in.web.staff.specialist.request.SpecialistUpdateForStaffRequest;
import com.medi.adapter.in.web.staff.specialist.request.PartnerOptionListForStaffRequest;
import com.medi.application.specialist.SpecialistForStaffService;
import com.medi.application.specialist.result.SpecialistDeletedResult;
import com.medi.common.security.AuthenticatedActor;
import com.medi.common.web.ApiResponse;
import com.medi.common.web.PaginatedResponse;
import com.medi.common.web.RequestTrace;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/staff/specialists")
public class SpecialistForStaffController {

	private final SpecialistForStaffService service;

	public SpecialistForStaffController(SpecialistForStaffService service) {
		this.service = service;
	}

	@GetMapping
	public ApiResponse list(
		@AuthenticationPrincipal AuthenticatedActor actor,
		@Valid @ModelAttribute SpecialistListForStaffRequest query,
		HttpServletRequest request
	) {
		PaginatedResponse<?> response = service.list(actor, query.toQuery());
		return ApiResponse.success(response.items(), response.meta(), RequestTrace.traceId(request));
	}

	@GetMapping("/partner-options")
	public ApiResponse partnerOptions(
		@AuthenticationPrincipal AuthenticatedActor actor,
		@Valid @ModelAttribute PartnerOptionListForStaffRequest query,
		HttpServletRequest request
	) {
		return ApiResponse.success(
			service.partnerOptions(actor, query.q(), query.limit()),
			RequestTrace.traceId(request)
		);
	}

	@GetMapping("/{id}")
	public ApiResponse get(
		@AuthenticationPrincipal AuthenticatedActor actor,
		@PathVariable Long id,
		HttpServletRequest request
	) {
		return ApiResponse.success(service.get(actor, id), RequestTrace.traceId(request));
	}

	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ApiResponse create(
		@AuthenticationPrincipal AuthenticatedActor actor,
		@Valid @ModelAttribute SpecialistCreateForStaffRequest body,
		HttpServletRequest request
	) {
		return ApiResponse.success(service.create(actor, body.toCommand()), RequestTrace.traceId(request));
	}

	@RequestMapping(
		value = "/{id}",
		method = {RequestMethod.POST, RequestMethod.PUT, RequestMethod.PATCH},
		consumes = MediaType.MULTIPART_FORM_DATA_VALUE
	)
	public ApiResponse update(
		@AuthenticationPrincipal AuthenticatedActor actor,
		@PathVariable Long id,
		@Valid @ModelAttribute SpecialistUpdateForStaffRequest body,
		HttpServletRequest request
	) {
		return ApiResponse.success(
			service.update(actor, id, body.toCommand(request.getParameterMap().keySet())),
			RequestTrace.traceId(request)
		);
	}

	@PatchMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ApiResponse patch(
		@AuthenticationPrincipal AuthenticatedActor actor,
		@PathVariable Long id,
		@Valid @RequestBody SpecialistStatusUpdateForStaffRequest body,
		HttpServletRequest request
	) {
		return ApiResponse.success(service.patch(actor, id, body.toCommand()), RequestTrace.traceId(request));
	}

	@DeleteMapping("/{id}")
	public ApiResponse delete(
		@AuthenticationPrincipal AuthenticatedActor actor,
		@PathVariable Long id,
		HttpServletRequest request
	) {
		SpecialistDeletedResult response = service.delete(actor, id);
		return ApiResponse.success(response, RequestTrace.traceId(request));
	}
}
