package com.medi.adapter.in.web.staff.doctor.controller;

import com.medi.adapter.in.web.staff.doctor.request.DoctorFormRequest;
import com.medi.adapter.in.web.staff.doctor.request.DoctorHospitalOptionsRequest;
import com.medi.adapter.in.web.staff.doctor.request.DoctorListRequest;
import com.medi.adapter.in.web.staff.doctor.request.DoctorPatchRequest;
import com.medi.application.doctor.DoctorStaffService;
import com.medi.application.doctor.result.DoctorDeletedResult;
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
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/staff/doctors")
public class DoctorStaffController {

	private final DoctorStaffService service;

	public DoctorStaffController(DoctorStaffService service) {
		this.service = service;
	}

	@GetMapping
	public ApiResponse list(
		@AuthenticationPrincipal AuthenticatedActor actor,
		@Valid @ModelAttribute DoctorListRequest query,
		HttpServletRequest request
	) {
		PaginatedResponse<?> response = service.list(actor, query.toQuery());
		return ApiResponse.success(response.items(), response.meta(), RequestTrace.traceId(request));
	}

	@GetMapping("/hospital-options")
	public ApiResponse hospitalOptions(
		@AuthenticationPrincipal AuthenticatedActor actor,
		@Valid @ModelAttribute DoctorHospitalOptionsRequest query,
		HttpServletRequest request
	) {
		return ApiResponse.success(
			service.hospitalOptions(actor, query.q(), query.limit()),
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
		@Valid @ModelAttribute DoctorFormRequest body,
		HttpServletRequest request
	) {
		return ApiResponse.success(service.create(actor, body.toCommand()), RequestTrace.traceId(request));
	}

	@PostMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ApiResponse update(
		@AuthenticationPrincipal AuthenticatedActor actor,
		@PathVariable Long id,
		@Valid @ModelAttribute DoctorFormRequest body,
		HttpServletRequest request
	) {
		return ApiResponse.success(service.update(actor, id, body.toCommand()), RequestTrace.traceId(request));
	}

	@PatchMapping("/{id}")
	public ApiResponse patch(
		@AuthenticationPrincipal AuthenticatedActor actor,
		@PathVariable Long id,
		@Valid @RequestBody DoctorPatchRequest body,
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
		DoctorDeletedResult response = service.delete(actor, id);
		return ApiResponse.success(response, RequestTrace.traceId(request));
	}
}
