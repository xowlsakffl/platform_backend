package com.platform.adapter.in.web.partner.invitation.controller;

import com.platform.adapter.in.web.partner.invitation.request.PartnerAccountInvitationAcceptRequest;
import com.platform.application.partner.PartnerAccountInvitationService;
import com.platform.common.web.ApiResponse;
import com.platform.common.web.RequestTrace;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/partner/account-invitations")
public class PartnerAccountInvitationController {

	private final PartnerAccountInvitationService service;

	public PartnerAccountInvitationController(PartnerAccountInvitationService service) {
		this.service = service;
	}

	@GetMapping("/verify")
	public ApiResponse verify(
		@RequestParam @Size(min = 32, max = 200) String token,
		HttpServletRequest request
	) {
		return ApiResponse.success(service.verify(token), RequestTrace.traceId(request));
	}

	@PostMapping("/accept")
	public ApiResponse accept(
		@Valid @RequestBody PartnerAccountInvitationAcceptRequest body,
		HttpServletRequest request
	) {
		return ApiResponse.success(service.accept(body.toCommand()), RequestTrace.traceId(request));
	}
}
