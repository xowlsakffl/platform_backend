package com.medi.adapter.in.web.staff.hospital.controller;

import com.medi.adapter.in.web.staff.hospital.request.HospitalAllowStatusUpdateForStaffRequest;
import com.medi.adapter.in.web.staff.hospital.request.HospitalCheckBusinessNumberForStaffRequest;
import com.medi.adapter.in.web.staff.hospital.request.HospitalCheckNameForStaffRequest;
import com.medi.adapter.in.web.staff.hospital.request.HospitalCreateForStaffRequest;
import com.medi.adapter.in.web.staff.hospital.request.HospitalGetForStaffRequest;
import com.medi.adapter.in.web.staff.hospital.request.HospitalListForStaffRequest;
import com.medi.adapter.in.web.staff.hospital.request.HospitalStatusUpdateForStaffRequest;
import com.medi.adapter.in.web.staff.hospital.request.HospitalUpdateForStaffRequest;
import com.medi.application.hospital.HospitalForStaffService;
import com.medi.application.hospital.result.HospitalDeletedResult;
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
@RequestMapping("/api/v1/staff/hospitals")
public class HospitalForStaffController {

	private final HospitalForStaffService service;

	public HospitalForStaffController(HospitalForStaffService service) {
		this.service = service;
	}

	@GetMapping
	public ApiResponse list(
		@AuthenticationPrincipal AuthenticatedActor actor,
		@Valid @ModelAttribute HospitalListForStaffRequest query,
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
		@Valid @ModelAttribute HospitalGetForStaffRequest query,
		HttpServletRequest request
	) {
		return ApiResponse.success(service.get(actor, id, query.toQuery()), RequestTrace.traceId(request));
	}

	@GetMapping("/{id}/operation-histories")
	public ApiResponse histories(
		@AuthenticationPrincipal AuthenticatedActor actor,
		@PathVariable Long id,
		@Valid @ModelAttribute HospitalGetForStaffRequest query,
		HttpServletRequest request
	) {
		PaginatedResponse<?> response = service.histories(actor, id, query.toHistoryQuery());
		return ApiResponse.success(response.items(), response.meta(), RequestTrace.traceId(request));
	}

	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ApiResponse create(
		@AuthenticationPrincipal AuthenticatedActor actor,
		@Valid @ModelAttribute HospitalCreateForStaffRequest body,
		HttpServletRequest request
	) {
		return ApiResponse.success(service.create(actor, body.toCommand()), RequestTrace.traceId(request));
	}

	@PostMapping("/check-name")
	public ApiResponse checkName(
		@AuthenticationPrincipal AuthenticatedActor actor,
		@Valid @RequestBody HospitalCheckNameForStaffRequest body,
		HttpServletRequest request
	) {
		return ApiResponse.success(service.checkName(actor, body.name()), RequestTrace.traceId(request));
	}

	@PostMapping("/check-business-number")
	public ApiResponse checkBusinessNumber(
		@AuthenticationPrincipal AuthenticatedActor actor,
		@Valid @RequestBody HospitalCheckBusinessNumberForStaffRequest body,
		HttpServletRequest request
	) {
		return ApiResponse.success(service.checkBusinessNumber(actor, body.businessNumber()), RequestTrace.traceId(request));
	}

	@RequestMapping(
		value = "/{id}",
		method = {RequestMethod.POST, RequestMethod.PUT, RequestMethod.PATCH},
		consumes = MediaType.MULTIPART_FORM_DATA_VALUE
	)
	public ApiResponse update(
		@AuthenticationPrincipal AuthenticatedActor actor,
		@PathVariable Long id,
		@Valid @ModelAttribute HospitalUpdateForStaffRequest body,
		HttpServletRequest request
	) {
		return ApiResponse.success(
			service.update(actor, id, body.toCommand(request.getParameterMap().keySet())),
			RequestTrace.traceId(request)
		);
	}

	@PatchMapping("/{id}/status")
	public ApiResponse updateStatus(
		@AuthenticationPrincipal AuthenticatedActor actor,
		@PathVariable Long id,
		@Valid @RequestBody HospitalStatusUpdateForStaffRequest body,
		HttpServletRequest request
	) {
		return ApiResponse.success(service.changeStatus(actor, id, body.toCommand()), RequestTrace.traceId(request));
	}

	@PatchMapping("/allow-status")
	public ApiResponse updateAllowStatus(
		@AuthenticationPrincipal AuthenticatedActor actor,
		@Valid @RequestBody HospitalAllowStatusUpdateForStaffRequest body,
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
