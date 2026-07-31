package com.medi.common.web.auth;

import com.medi.application.auth.command.AuthClientContext;
import jakarta.servlet.http.HttpServletRequest;

public final class AuthRequestSupport {

	private AuthRequestSupport() {
	}

	public static AuthClientContext clientContext(HttpServletRequest request) {
		return new AuthClientContext(request.getRemoteAddr(), request.getHeader("User-Agent"));
	}
}
