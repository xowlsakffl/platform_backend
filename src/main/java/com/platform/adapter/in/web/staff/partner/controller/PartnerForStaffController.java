package com.platform.adapter.in.web.staff.partner.controller;

import com.platform.adapter.in.web.staff.partner.request.PartnerAllowStatusUpdateForStaffRequest;
import com.platform.adapter.in.web.staff.partner.request.PartnerAllowStatusReviewForStaffRequest;
import com.platform.adapter.in.web.staff.partner.request.PartnerAccountStatusUpdateForStaffRequest;
import com.platform.adapter.in.web.staff.partner.request.PartnerAssignedStaffUpdateForStaffRequest;
import com.platform.adapter.in.web.staff.partner.request.PartnerCheckBusinessNumberForStaffRequest;
import com.platform.adapter.in.web.staff.partner.request.PartnerCreateForStaffRequest;
import com.platform.adapter.in.web.staff.partner.request.PartnerGetForStaffRequest;
import com.platform.adapter.in.web.staff.partner.request.PartnerListForStaffRequest;
import com.platform.adapter.in.web.staff.partner.request.PartnerOptionCreateForStaffRequest;
import com.platform.adapter.in.web.staff.partner.request.PartnerStatusUpdateForStaffRequest;
import com.platform.adapter.in.web.staff.partner.request.PartnerUpdateForStaffRequest;
import com.platform.application.partner.PartnerForStaffService;
import com.platform.application.partner.result.PartnerDeletedResult;
import com.platform.common.security.AuthenticatedActor;
import com.platform.common.web.ApiResponse;
import com.platform.common.web.PaginatedResponse;
import com.platform.common.web.RequestTrace;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.util.List;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/staff/partners")
public class PartnerForStaffController {

	private final PartnerForStaffService service;

	public PartnerForStaffController(PartnerForStaffService service) {
		this.service = service;
	}

	@GetMapping
	public ApiResponse list(
		@AuthenticationPrincipal AuthenticatedActor actor,
		@Valid @ModelAttribute PartnerListForStaffRequest query,
		HttpServletRequest request
	) {
		PaginatedResponse<?> response = service.list(actor, query.toQuery());

		return ApiResponse.success(response.items(), response.meta(), RequestTrace.traceId(request));
	}

	@GetMapping("/summary")
	public ApiResponse summary(@AuthenticationPrincipal AuthenticatedActor actor, HttpServletRequest request) {
		return ApiResponse.success(service.summary(actor), RequestTrace.traceId(request));
	}

	@GetMapping("/assigned-staff-options")
	public ApiResponse assignedStaffOptions(
		@AuthenticationPrincipal AuthenticatedActor actor,
		@RequestParam(required = false) String q,
		HttpServletRequest request
	) {
		return ApiResponse.success(service.assignedStaffOptions(actor, q), RequestTrace.traceId(request));
	}

	@GetMapping("/{id}")
	public ApiResponse get(
		@AuthenticationPrincipal AuthenticatedActor actor,
		@PathVariable Long id,
		@Valid @ModelAttribute PartnerGetForStaffRequest query,
		HttpServletRequest request
	) {
		return ApiResponse.success(service.get(actor, id, query.toQuery()), RequestTrace.traceId(request));
	}

	@GetMapping("/{id}/operation-histories")
	public ApiResponse histories(
		@AuthenticationPrincipal AuthenticatedActor actor,
		@PathVariable Long id,
		@Valid @ModelAttribute PartnerGetForStaffRequest query,
		HttpServletRequest request
	) {
		PaginatedResponse<?> response = service.histories(actor, id, query.toHistoryQuery());
		return ApiResponse.success(response.items(), response.meta(), RequestTrace.traceId(request));
	}

	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ApiResponse create(
		@AuthenticationPrincipal AuthenticatedActor actor,
		@Valid @ModelAttribute PartnerCreateForStaffRequest body,
		@RequestPart(name = "options", required = false) @Size(max = 100)
		List<@Valid PartnerOptionCreateForStaffRequest> options,
		HttpServletRequest request
	) {
		return ApiResponse.success(service.create(actor, body.toCommand(options)), RequestTrace.traceId(request));
	}

	@PostMapping("/check-business-number")
	public ApiResponse checkBusinessNumber(
		@AuthenticationPrincipal AuthenticatedActor actor,
		@Valid @RequestBody PartnerCheckBusinessNumberForStaffRequest body,
		HttpServletRequest request
	) {
		return ApiResponse.success(service.checkBusinessNumber(actor, body.businessNumber()), RequestTrace.traceId(request));
	}

	@PatchMapping(value = "/{id}/fields", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ApiResponse updateFields(
		@AuthenticationPrincipal AuthenticatedActor actor,
		@PathVariable Long id,
		@Valid @ModelAttribute PartnerUpdateForStaffRequest body,
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
		@Valid @RequestBody PartnerStatusUpdateForStaffRequest body,
		HttpServletRequest request
	) {
		return ApiResponse.success(service.changeStatus(actor, id, body.toCommand()), RequestTrace.traceId(request));
	}

	@PatchMapping("/{id}/account-status")
	public ApiResponse updateAccountStatus(
		@AuthenticationPrincipal AuthenticatedActor actor,
		@PathVariable Long id,
		@Valid @RequestBody PartnerAccountStatusUpdateForStaffRequest body,
		HttpServletRequest request
	) {
		return ApiResponse.success(
			service.changeAccountStatus(actor, id, body.toCommand()),
			RequestTrace.traceId(request)
		);
	}

	@PatchMapping("/{id}/assigned-staff")
	public ApiResponse updateAssignedStaff(
		@AuthenticationPrincipal AuthenticatedActor actor,
		@PathVariable Long id,
		@Valid @RequestBody PartnerAssignedStaffUpdateForStaffRequest body,
		HttpServletRequest request
	) {
		return ApiResponse.success(
			service.changeAssignedStaff(actor, id, body.assignedStaffId()),
			RequestTrace.traceId(request)
		);
	}

	@PatchMapping("/allow-status")
	public ApiResponse updateAllowStatus(
		@AuthenticationPrincipal AuthenticatedActor actor,
		@Valid @RequestBody PartnerAllowStatusUpdateForStaffRequest body,
		HttpServletRequest request
	) {
		return ApiResponse.success(service.changeAllowStatus(actor, body.toCommand()), RequestTrace.traceId(request));
	}

	@PatchMapping("/{id}/allow-status")
	public ApiResponse reviewAllowStatus(
		@AuthenticationPrincipal AuthenticatedActor actor,
		@PathVariable Long id,
		@Valid @RequestBody PartnerAllowStatusReviewForStaffRequest body,
		HttpServletRequest request
	) {
		return ApiResponse.success(
			service.reviewAllowStatus(actor, id, body.allowStatus(), body.reason()),
			RequestTrace.traceId(request)
		);
	}

	@DeleteMapping("/{id}")
	public ApiResponse delete(
		@AuthenticationPrincipal AuthenticatedActor actor,
		@PathVariable Long id,
		HttpServletRequest request
	) {
		PartnerDeletedResult response = service.delete(actor, id);
		return ApiResponse.success(response, RequestTrace.traceId(request));
	}
}
