package com.medi.adapter.in.web.user.auth.controller;

import com.medi.adapter.in.web.user.auth.request.UserLoginRequest;
import com.medi.application.auth.AuthenticationService;
import com.medi.common.security.AuthenticatedActor;
import com.medi.common.security.BearerTokenResolver;
import com.medi.common.web.ApiResponse;
import com.medi.common.web.RequestTrace;
import com.medi.domain.account.AccountActorType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/user/auth")
public class UserAuthController {

	private final AuthenticationService authenticationService;

	public UserAuthController(AuthenticationService authenticationService) {
		this.authenticationService = authenticationService;
	}

	@PostMapping("/login")
	public ApiResponse login(@Valid @RequestBody UserLoginRequest body, HttpServletRequest request) {
		return ApiResponse.success(
			authenticationService.login(AccountActorType.USER, body.toCommand()),
			RequestTrace.traceId(request)
		);
	}

	@GetMapping("/me")
	public ApiResponse me(@AuthenticationPrincipal AuthenticatedActor actor, HttpServletRequest request) {
		return ApiResponse.success(
			authenticationService.me(AccountActorType.USER, actor),
			RequestTrace.traceId(request)
		);
	}

	@PostMapping("/logout")
	public ApiResponse logout(@AuthenticationPrincipal AuthenticatedActor actor, HttpServletRequest request) {
		return ApiResponse.success(
			authenticationService.logout(AccountActorType.USER, actor, BearerTokenResolver.resolve(request)),
			RequestTrace.traceId(request)
		);
	}
}
