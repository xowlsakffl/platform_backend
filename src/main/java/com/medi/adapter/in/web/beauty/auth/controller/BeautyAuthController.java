package com.medi.adapter.in.web.beauty.auth.controller;

import com.medi.adapter.in.web.beauty.auth.request.BeautyLoginRequest;
import com.medi.common.security.AuthenticatedActor;
import com.medi.common.web.ApiResponse;
import com.medi.common.web.RequestTrace;
import com.medi.common.web.auth.AuthRequestSupport;
import com.medi.common.web.auth.AuthWebService;
import com.medi.common.web.auth.request.PasswordResetLinkRequest;
import com.medi.common.web.auth.request.PasswordResetRequest;
import com.medi.common.web.auth.request.PasswordResetTokenVerifyRequest;
import com.medi.domain.account.AccountActorType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/beauty/auth")
public class BeautyAuthController {

	private static final AccountActorType ACTOR_TYPE = AccountActorType.BEAUTY;
	private final AuthWebService authWebService;

	public BeautyAuthController(AuthWebService authWebService) {
		this.authWebService = authWebService;
	}

	@PostMapping("/login")
	public ApiResponse login(
		@Valid @RequestBody BeautyLoginRequest body,
		HttpServletRequest request,
		HttpServletResponse response
	) {
		return ApiResponse.success(
			authWebService.login(ACTOR_TYPE, body.toCommand(AuthRequestSupport.clientContext(request)), response),
			RequestTrace.traceId(request)
		);
	}

	@PostMapping("/refresh")
	public ApiResponse refresh(HttpServletRequest request, HttpServletResponse response) {
		return ApiResponse.success(
			authWebService.refresh(ACTOR_TYPE, request, response),
			RequestTrace.traceId(request)
		);
	}

	@PostMapping("/password-reset-link")
	public ApiResponse sendPasswordResetLink(
		@Valid @RequestBody PasswordResetLinkRequest body,
		HttpServletRequest request
	) {
		return ApiResponse.success(
			authWebService.sendPasswordResetLink(ACTOR_TYPE, body, request),
			RequestTrace.traceId(request)
		);
	}

	@PostMapping("/password-reset/verify")
	public ApiResponse verifyPasswordResetToken(
		@Valid @RequestBody PasswordResetTokenVerifyRequest body,
		HttpServletRequest request
	) {
		return ApiResponse.success(
			authWebService.verifyPasswordResetToken(ACTOR_TYPE, body, request),
			RequestTrace.traceId(request)
		);
	}

	@PostMapping("/password-reset")
	public ApiResponse resetPassword(
		@Valid @RequestBody PasswordResetRequest body,
		HttpServletRequest request
	) {
		return ApiResponse.success(
			authWebService.resetPassword(ACTOR_TYPE, body, request),
			RequestTrace.traceId(request)
		);
	}

	@GetMapping("/me")
	public ApiResponse me(@AuthenticationPrincipal AuthenticatedActor actor, HttpServletRequest request) {
		return ApiResponse.success(authWebService.me(ACTOR_TYPE, actor), RequestTrace.traceId(request));
	}

	@PostMapping("/logout")
	public ApiResponse logout(
		@AuthenticationPrincipal AuthenticatedActor actor,
		HttpServletRequest request,
		HttpServletResponse response
	) {
		return ApiResponse.success(
			authWebService.logout(ACTOR_TYPE, actor, request, response),
			RequestTrace.traceId(request)
		);
	}

	@PostMapping("/logout-all")
	public ApiResponse logoutAll(
		@AuthenticationPrincipal AuthenticatedActor actor,
		HttpServletRequest request,
		HttpServletResponse response
	) {
		return ApiResponse.success(
			authWebService.logoutAll(ACTOR_TYPE, actor, request, response),
			RequestTrace.traceId(request)
		);
	}
}
