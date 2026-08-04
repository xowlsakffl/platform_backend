package com.platform.adapter.in.web.staff.partner.controller;

import com.platform.adapter.in.web.staff.partner.request.PartnerAccountInvitationCreateForStaffRequest;
import com.platform.application.partner.PartnerAccountInvitationService;
import com.platform.common.security.AuthenticatedActor;
import com.platform.common.web.ApiResponse;
import com.platform.common.web.RequestTrace;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/staff/partners/{partnerId}/account-invitations")
public class PartnerAccountInvitationForStaffController {

	private final PartnerAccountInvitationService service;

	public PartnerAccountInvitationForStaffController(PartnerAccountInvitationService service) {
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

	@PostMapping
	public ApiResponse invite(
		@AuthenticationPrincipal AuthenticatedActor actor,
		@PathVariable Long partnerId,
		@Valid @RequestBody PartnerAccountInvitationCreateForStaffRequest body,
		HttpServletRequest request
	) {
		return ApiResponse.success(
			service.invite(actor, partnerId, body.toCommand()),
			RequestTrace.traceId(request)
		);
	}

	@PostMapping("/{invitationId}/resend")
	public ApiResponse resend(
		@AuthenticationPrincipal AuthenticatedActor actor,
		@PathVariable Long partnerId,
		@PathVariable Long invitationId,
		HttpServletRequest request
	) {
		return ApiResponse.success(
			service.resend(actor, partnerId, invitationId),
			RequestTrace.traceId(request)
		);
	}

	@DeleteMapping("/{invitationId}")
	public ApiResponse cancel(
		@AuthenticationPrincipal AuthenticatedActor actor,
		@PathVariable Long partnerId,
		@PathVariable Long invitationId,
		HttpServletRequest request
	) {
		return ApiResponse.success(
			service.cancel(actor, partnerId, invitationId),
			RequestTrace.traceId(request)
		);
	}
}
