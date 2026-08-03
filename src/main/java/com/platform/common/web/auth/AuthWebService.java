package com.platform.common.web.auth;

import com.platform.application.auth.AuthenticationService;
import com.platform.application.auth.PasswordResetService;
import com.platform.application.auth.command.AuthLoginCommand;
import com.platform.application.auth.result.AuthActorResult;
import com.platform.application.auth.result.AuthLogoutResult;
import com.platform.application.auth.result.AuthSessionTokenResult;
import com.platform.application.auth.result.AuthTokenResult;
import com.platform.application.auth.result.PasswordResetMessageResult;
import com.platform.application.auth.result.PasswordResetTokenVerifyResult;
import com.platform.common.web.auth.request.PasswordResetLinkRequest;
import com.platform.common.web.auth.request.PasswordResetRequest;
import com.platform.common.web.auth.request.PasswordResetTokenVerifyRequest;
import com.platform.common.security.AuthenticatedActor;
import com.platform.common.security.BearerTokenResolver;
import com.platform.domain.account.AccountActorType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

@Component
public class AuthWebService {

	private final AuthenticationService authenticationService;
	private final PasswordResetService passwordResetService;
	private final AuthRefreshCookieManager cookieManager;

	public AuthWebService(
		AuthenticationService authenticationService,
		PasswordResetService passwordResetService,
		AuthRefreshCookieManager cookieManager
	) {
		this.authenticationService = authenticationService;
		this.passwordResetService = passwordResetService;
		this.cookieManager = cookieManager;
	}

	public AuthTokenResult login(
		AccountActorType actorType,
		AuthLoginCommand command,
		HttpServletResponse response
	) {
		AuthSessionTokenResult result = authenticationService.login(actorType, command);
		writeRefreshCookie(actorType, result, response);
		return result.token();
	}

	public AuthTokenResult refresh(
		AccountActorType actorType,
		HttpServletRequest request,
		HttpServletResponse response
	) {
		AuthSessionTokenResult result = authenticationService.refresh(
			actorType,
			cookieManager.require(actorType, request),
			AuthRequestSupport.clientContext(request)
		);
		writeRefreshCookie(actorType, result, response);
		return result.token();
	}

	public AuthActorResult me(AccountActorType actorType, AuthenticatedActor actor) {
		return authenticationService.me(actorType, actor);
	}

	public PasswordResetMessageResult sendPasswordResetLink(
		AccountActorType actorType,
		PasswordResetLinkRequest body,
		HttpServletRequest request
	) {
		return passwordResetService.sendLink(
			actorType,
			body.toCommand(AuthRequestSupport.clientContext(request))
		);
	}

	public PasswordResetTokenVerifyResult verifyPasswordResetToken(
		AccountActorType actorType,
		PasswordResetTokenVerifyRequest body,
		HttpServletRequest request
	) {
		return passwordResetService.verify(
			actorType,
			body.toCommand(AuthRequestSupport.clientContext(request))
		);
	}

	public PasswordResetMessageResult resetPassword(
		AccountActorType actorType,
		PasswordResetRequest body,
		HttpServletRequest request
	) {
		return passwordResetService.reset(
			actorType,
			body.toCommand(AuthRequestSupport.clientContext(request))
		);
	}

	public AuthLogoutResult logout(
		AccountActorType actorType,
		AuthenticatedActor actor,
		HttpServletRequest request,
		HttpServletResponse response
	) {
		try {
			return authenticationService.logout(actorType, actor, BearerTokenResolver.resolve(request));
		} finally {
			cookieManager.clear(actorType, response);
		}
	}

	public AuthLogoutResult logoutAll(
		AccountActorType actorType,
		AuthenticatedActor actor,
		HttpServletRequest request,
		HttpServletResponse response
	) {
		try {
			return authenticationService.logoutAll(actorType, actor, BearerTokenResolver.resolve(request));
		} finally {
			cookieManager.clear(actorType, response);
		}
	}

	private void writeRefreshCookie(
		AccountActorType actorType,
		AuthSessionTokenResult result,
		HttpServletResponse response
	) {
		cookieManager.write(
			actorType,
			result.refreshToken(),
			result.refreshExpiresIn(),
			result.persistent(),
			response
		);
	}
}
