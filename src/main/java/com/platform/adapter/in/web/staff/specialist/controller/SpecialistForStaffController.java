package com.platform.adapter.in.web.staff.specialist.controller;

import com.platform.adapter.in.web.staff.specialist.request.SpecialistCreateForStaffRequest;
import com.platform.adapter.in.web.staff.specialist.request.SpecialistOrderUpdateForStaffRequest;
import com.platform.adapter.in.web.staff.specialist.request.SpecialistStatusUpdateForStaffRequest;
import com.platform.adapter.in.web.staff.specialist.request.SpecialistUpdateForStaffRequest;
import com.platform.application.specialist.SpecialistForStaffService;
import com.platform.application.specialist.result.SpecialistDeletedResult;
import com.platform.common.security.AuthenticatedActor;
import com.platform.common.web.ApiResponse;
import com.platform.common.web.RequestTrace;
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
@RequestMapping("/api/v1/staff/partners/{partnerId}/specialists")
public class SpecialistForStaffController {

	private final SpecialistForStaffService service;

	public SpecialistForStaffController(SpecialistForStaffService service) {
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

	@GetMapping("/{specialistId}")
	public ApiResponse get(
		@AuthenticationPrincipal AuthenticatedActor actor,
		@PathVariable Long partnerId,
		@PathVariable Long specialistId,
		HttpServletRequest request
	) {
		return ApiResponse.success(
			service.get(actor, partnerId, specialistId),
			RequestTrace.traceId(request)
		);
	}

	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ApiResponse create(
		@AuthenticationPrincipal AuthenticatedActor actor,
		@PathVariable Long partnerId,
		@Valid @ModelAttribute SpecialistCreateForStaffRequest body,
		HttpServletRequest request
	) {
		return ApiResponse.success(
			service.create(actor, partnerId, body.toCommand(partnerId)),
			RequestTrace.traceId(request)
		);
	}

	@RequestMapping(
		value = "/{specialistId}",
		method = {RequestMethod.POST, RequestMethod.PUT, RequestMethod.PATCH},
		consumes = MediaType.MULTIPART_FORM_DATA_VALUE
	)
	public ApiResponse update(
		@AuthenticationPrincipal AuthenticatedActor actor,
		@PathVariable Long partnerId,
		@PathVariable Long specialistId,
		@Valid @ModelAttribute SpecialistUpdateForStaffRequest body,
		HttpServletRequest request
	) {
		return ApiResponse.success(
			service.update(actor, partnerId, specialistId, body.toCommand(request.getParameterMap().keySet())),
			RequestTrace.traceId(request)
		);
	}

	@PatchMapping(value = "/{specialistId}", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ApiResponse patch(
		@AuthenticationPrincipal AuthenticatedActor actor,
		@PathVariable Long partnerId,
		@PathVariable Long specialistId,
		@Valid @RequestBody SpecialistStatusUpdateForStaffRequest body,
		HttpServletRequest request
	) {
		return ApiResponse.success(
			service.patch(actor, partnerId, specialistId, body.toCommand()),
			RequestTrace.traceId(request)
		);
	}

	@PatchMapping(value = "/order", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ApiResponse reorder(
		@AuthenticationPrincipal AuthenticatedActor actor,
		@PathVariable Long partnerId,
		@Valid @RequestBody SpecialistOrderUpdateForStaffRequest body,
		HttpServletRequest request
	) {
		return ApiResponse.success(
			service.reorder(actor, partnerId, body.toCommand()),
			RequestTrace.traceId(request)
		);
	}

	@DeleteMapping("/{specialistId}")
	public ApiResponse delete(
		@AuthenticationPrincipal AuthenticatedActor actor,
		@PathVariable Long partnerId,
		@PathVariable Long specialistId,
		HttpServletRequest request
	) {
		SpecialistDeletedResult response = service.delete(actor, partnerId, specialistId);
		return ApiResponse.success(response, RequestTrace.traceId(request));
	}
}
