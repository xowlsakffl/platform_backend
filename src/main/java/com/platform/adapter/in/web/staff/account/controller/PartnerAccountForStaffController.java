package com.platform.adapter.in.web.staff.account.controller;

import com.platform.adapter.in.web.staff.account.request.PartnerAccountListForStaffRequest;
import com.platform.adapter.in.web.staff.account.request.PartnerAccountHistoryForStaffRequest;
import com.platform.adapter.in.web.staff.account.request.PartnerAccountPasswordResetLinkForStaffRequest;
import com.platform.adapter.in.web.staff.account.request.PartnerAccountStatusUpdateForStaffRequest;
import com.platform.application.account.PartnerAccountForStaffService;
import com.platform.common.security.AuthenticatedActor;
import com.platform.common.web.ApiResponse;
import com.platform.common.web.PaginatedResponse;
import com.platform.common.web.RequestTrace;
import com.platform.common.web.auth.AuthRequestSupport;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/staff/partner-accounts")
public class PartnerAccountForStaffController {

	private final PartnerAccountForStaffService service;

	public PartnerAccountForStaffController(PartnerAccountForStaffService service) {
		this.service = service;
	}

	@GetMapping
	public ApiResponse list(
		@AuthenticationPrincipal AuthenticatedActor actor,
		@Valid @ModelAttribute PartnerAccountListForStaffRequest query,
		HttpServletRequest request
	) {
		PaginatedResponse<?> response = service.list(actor, query.toQuery());
		return ApiResponse.success(response.items(), response.meta(), RequestTrace.traceId(request));
	}

	@GetMapping("/{id}")
	public ApiResponse get(
		@AuthenticationPrincipal AuthenticatedActor actor,
		@PathVariable Long id,
		HttpServletRequest request
	) {
		return ApiResponse.success(service.get(actor, id), RequestTrace.traceId(request));
	}

	@PatchMapping("/{id}/status")
	public ApiResponse updateStatus(
		@AuthenticationPrincipal AuthenticatedActor actor,
		@PathVariable Long id,
		@Valid @RequestBody PartnerAccountStatusUpdateForStaffRequest body,
		HttpServletRequest request
	) {
		return ApiResponse.success(
			service.changeStatus(actor, id, body.toCommand(), AuthRequestSupport.clientContext(request)),
			RequestTrace.traceId(request)
		);
	}

	@PostMapping("/{id}/password-reset-link")
	public ApiResponse sendPasswordResetLink(
		@AuthenticationPrincipal AuthenticatedActor actor,
		@PathVariable Long id,
		@Valid @RequestBody PartnerAccountPasswordResetLinkForStaffRequest body,
		HttpServletRequest request
	) {
		return ApiResponse.success(
			service.sendPasswordResetLink(actor, id, body.email(), AuthRequestSupport.clientContext(request)),
			RequestTrace.traceId(request)
		);
	}

	@GetMapping("/{id}/security")
	public ApiResponse security(
		@AuthenticationPrincipal AuthenticatedActor actor,
		@PathVariable Long id,
		HttpServletRequest request
	) {
		return ApiResponse.success(service.security(actor, id), RequestTrace.traceId(request));
	}

	@PostMapping("/{id}/login-lock/unlock")
	public ApiResponse clearLoginLock(
		@AuthenticationPrincipal AuthenticatedActor actor,
		@PathVariable Long id,
		HttpServletRequest request
	) {
		return ApiResponse.success(
			service.clearLoginLock(actor, id, AuthRequestSupport.clientContext(request)),
			RequestTrace.traceId(request)
		);
	}

	@DeleteMapping("/{id}/sessions/{sessionId}")
	public ApiResponse revokeSession(
		@AuthenticationPrincipal AuthenticatedActor actor,
		@PathVariable Long id,
		@PathVariable String sessionId,
		HttpServletRequest request
	) {
		return ApiResponse.success(
			service.revokeSession(actor, id, sessionId, AuthRequestSupport.clientContext(request)),
			RequestTrace.traceId(request)
		);
	}


	@GetMapping("/{id}/access-events")
	public ApiResponse accessEvents(
		@AuthenticationPrincipal AuthenticatedActor actor,
		@PathVariable Long id,
		@Valid @ModelAttribute PartnerAccountHistoryForStaffRequest query,
		HttpServletRequest request
	) {
		PaginatedResponse<?> response = service.accessEvents(actor, id, query.pageValue(), query.perPageValue());
		return ApiResponse.success(response.items(), response.meta(), RequestTrace.traceId(request));
	}

	@GetMapping("/{id}/management-histories")
	public ApiResponse managementHistories(
		@AuthenticationPrincipal AuthenticatedActor actor,
		@PathVariable Long id,
		@Valid @ModelAttribute PartnerAccountHistoryForStaffRequest query,
		HttpServletRequest request
	) {
		PaginatedResponse<?> response = service.managementHistories(actor, id, query.pageValue(), query.perPageValue());
		return ApiResponse.success(response.items(), response.meta(), RequestTrace.traceId(request));
	}
}
