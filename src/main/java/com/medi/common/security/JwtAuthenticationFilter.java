package com.medi.common.security;

import com.medi.application.auth.AuthenticationService;
import com.medi.common.error.ApiException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private static final String AUTHORIZATION = "Authorization";
	private static final String BEARER = "Bearer ";

	private final JwtTokenService jwtTokenService;
	private final AuthenticationService authenticationService;

	public JwtAuthenticationFilter(JwtTokenService jwtTokenService, AuthenticationService authenticationService) {
		this.jwtTokenService = jwtTokenService;
		this.authenticationService = authenticationService;
	}

	@Override
	protected void doFilterInternal(
		HttpServletRequest request,
		HttpServletResponse response,
		FilterChain filterChain
	) throws ServletException, IOException {
		String token = bearerToken(request);
		if (token == null) {
			filterChain.doFilter(request, response);
			return;
		}

		try {
			AuthenticatedActor actor = authenticationService.authenticate(jwtTokenService.parse(token));
			UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
				actor,
				token,
				actor.authorities()
			);
			authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
			SecurityContextHolder.getContext().setAuthentication(authentication);
		} catch (ApiException exception) {
			SecurityContextHolder.clearContext();
		}

		filterChain.doFilter(request, response);
	}

	private String bearerToken(HttpServletRequest request) {
		String authorization = request.getHeader(AUTHORIZATION);
		if (authorization == null || !authorization.startsWith(BEARER)) {
			return null;
		}
		String token = authorization.substring(BEARER.length()).trim();
		return token.isBlank() ? null : token;
	}
}
