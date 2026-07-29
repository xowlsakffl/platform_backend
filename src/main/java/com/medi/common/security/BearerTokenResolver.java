package com.medi.common.security;

import jakarta.servlet.http.HttpServletRequest;

public final class BearerTokenResolver {

	private static final String AUTHORIZATION = "Authorization";
	private static final String BEARER = "Bearer ";

	private BearerTokenResolver() {
	}

	public static String resolve(HttpServletRequest request) {
		String authorization = request.getHeader(AUTHORIZATION);
		if (authorization == null || !authorization.startsWith(BEARER)) {
			return null;
		}
		String token = authorization.substring(BEARER.length()).trim();
		return token.isBlank() ? null : token;
	}
}
