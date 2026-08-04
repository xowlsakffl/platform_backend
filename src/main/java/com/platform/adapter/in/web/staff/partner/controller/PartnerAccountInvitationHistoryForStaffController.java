package com.platform.adapter.in.web.staff.partner.controller;

import com.platform.adapter.in.web.staff.partner.request.PartnerAccountInvitationListForStaffRequest;
import com.platform.application.partner.PartnerAccountInvitationService;
import com.platform.common.security.AuthenticatedActor;
import com.platform.common.web.ApiResponse;
import com.platform.common.web.PaginatedResponse;
import com.platform.common.web.RequestTrace;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/staff/partner-account-invitations")
public class PartnerAccountInvitationHistoryForStaffController {

	private final PartnerAccountInvitationService service;

	public PartnerAccountInvitationHistoryForStaffController(PartnerAccountInvitationService service) {
		this.service = service;
	}

	@GetMapping
	public ApiResponse list(
		@AuthenticationPrincipal AuthenticatedActor actor,
		@Valid @ModelAttribute PartnerAccountInvitationListForStaffRequest query,
		HttpServletRequest request
	) {
		PaginatedResponse<?> response = service.listAll(actor, query.toQuery());
		return ApiResponse.success(response.items(), response.meta(), RequestTrace.traceId(request));
	}
}
