package com.medi.adapter.in.web.staff.hospital.controller;

import com.medi.adapter.in.web.staff.hospital.request.HospitalAllowStatusUpdateRequest;
import com.medi.adapter.in.web.staff.hospital.request.HospitalCheckBusinessNumberRequest;
import com.medi.adapter.in.web.staff.hospital.request.HospitalCheckNameRequest;
import com.medi.adapter.in.web.staff.hospital.request.HospitalCreateRequest;
import com.medi.adapter.in.web.staff.hospital.request.HospitalListRequest;
import com.medi.adapter.in.web.staff.hospital.request.HospitalStatusUpdateRequest;
import com.medi.adapter.in.web.staff.hospital.request.HospitalUpdateRequest;
import com.medi.application.hospital.HospitalStaffService;
import com.medi.application.hospital.result.HospitalDeletedResult;
import com.medi.common.security.AuthenticatedActor;
import com.medi.common.web.ApiResponse;
import com.medi.common.web.PaginatedResponse;
import com.medi.common.web.RequestTrace;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
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
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/staff/hospitals")
public class HospitalStaffController {

	private final HospitalStaffService service;

	public HospitalStaffController(HospitalStaffService service) {
		this.service = service;
	}

	@GetMapping
	public ApiResponse list(
		@AuthenticationPrincipal AuthenticatedActor actor,
		@Valid @ModelAttribute HospitalListRequest query,
		HttpServletRequest request
	) {
		PaginatedResponse<?> response = service.list(actor, query.toQuery());

		return ApiResponse.success(response.items(), response.meta(), RequestTrace.traceId(request));
	}

	@GetMapping("/summary")
	public ApiResponse summary(@AuthenticationPrincipal AuthenticatedActor actor, HttpServletRequest request) {
		return ApiResponse.success(service.summary(actor), RequestTrace.traceId(request));
	}

	@GetMapping("/{id}")
	public ApiResponse get(
		@AuthenticationPrincipal AuthenticatedActor actor,
		@PathVariable Long id,
		HttpServletRequest request
	) {
		return ApiResponse.success(service.get(actor, id), RequestTrace.traceId(request));
	}

	@GetMapping("/{id}/operation-histories")
	public ApiResponse histories(
		@AuthenticationPrincipal AuthenticatedActor actor,
		@PathVariable Long id,
		HttpServletRequest request
	) {
		return ApiResponse.success(service.histories(actor, id), RequestTrace.traceId(request));
	}

	@PostMapping
	public ApiResponse create(
		@AuthenticationPrincipal AuthenticatedActor actor,
		@Valid @RequestBody HospitalCreateRequest body,
		HttpServletRequest request
	) {
		return ApiResponse.success(service.create(actor, body.toCommand()), RequestTrace.traceId(request));
	}

	@PostMapping("/check-name")
	public ApiResponse checkName(
		@AuthenticationPrincipal AuthenticatedActor actor,
		@Valid @RequestBody HospitalCheckNameRequest body,
		HttpServletRequest request
	) {
		return ApiResponse.success(service.checkName(actor, body.name()), RequestTrace.traceId(request));
	}

	@PostMapping("/check-business-number")
	public ApiResponse checkBusinessNumber(
		@AuthenticationPrincipal AuthenticatedActor actor,
		@Valid @RequestBody HospitalCheckBusinessNumberRequest body,
		HttpServletRequest request
	) {
		return ApiResponse.success(service.checkBusinessNumber(actor, body.businessNumber()), RequestTrace.traceId(request));
	}

	@PatchMapping("/{id}")
	public ApiResponse update(
		@AuthenticationPrincipal AuthenticatedActor actor,
		@PathVariable Long id,
		@Valid @RequestBody HospitalUpdateRequest body,
		HttpServletRequest request
	) {
		return ApiResponse.success(service.update(actor, id, body.toCommand()), RequestTrace.traceId(request));
	}

	@PatchMapping("/{id}/status")
	public ApiResponse updateStatus(
		@AuthenticationPrincipal AuthenticatedActor actor,
		@PathVariable Long id,
		@Valid @RequestBody HospitalStatusUpdateRequest body,
		HttpServletRequest request
	) {
		return ApiResponse.success(service.changeStatus(actor, id, body.toCommand()), RequestTrace.traceId(request));
	}

	@PatchMapping("/allow-status")
	public ApiResponse updateAllowStatus(
		@AuthenticationPrincipal AuthenticatedActor actor,
		@Valid @RequestBody HospitalAllowStatusUpdateRequest body,
		HttpServletRequest request
	) {
		return ApiResponse.success(service.changeAllowStatus(actor, body.toCommand()), RequestTrace.traceId(request));
	}

	@DeleteMapping("/{id}")
	public ApiResponse delete(
		@AuthenticationPrincipal AuthenticatedActor actor,
		@PathVariable Long id,
		HttpServletRequest request
	) {
		HospitalDeletedResult response = service.delete(actor, id);
		return ApiResponse.success(response, RequestTrace.traceId(request));
	}
}
