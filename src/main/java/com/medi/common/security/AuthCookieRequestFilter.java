package com.medi.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medi.common.error.ErrorCode;
import com.medi.common.web.ApiError;
import com.medi.common.web.ApiResponse;
import com.medi.common.web.RequestTrace;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class AuthCookieRequestFilter extends OncePerRequestFilter {

	public static final String HEADER_NAME = "X-Auth-Request";
	public static final String HEADER_VALUE = "medi-web";

	private final ObjectMapper objectMapper;

	public AuthCookieRequestFilter(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	@Override
	protected void doFilterInternal(
		HttpServletRequest request,
		HttpServletResponse response,
		FilterChain filterChain
	) throws ServletException, IOException {
		if (requiresProtection(request) && !HEADER_VALUE.equals(request.getHeader(HEADER_NAME))) {
			writeForbidden(request, response);
			return;
		}
		filterChain.doFilter(request, response);
	}

	private boolean requiresProtection(HttpServletRequest request) {
		if (!"POST".equalsIgnoreCase(request.getMethod())) {
			return false;
		}
		String uri = request.getRequestURI();
		if (!uri.matches(
			"/api/v1/(staff|partner|beauty|user)/auth/"
				+ "(login|refresh|logout|logout-all|password-reset-link|password-reset|password-reset/verify)"
		)) {
			return false;
		}
		return true;
	}

	private void writeForbidden(HttpServletRequest request, HttpServletResponse response) throws IOException {
		response.setStatus(ErrorCode.FORBIDDEN.httpStatus().value());
		response.setCharacterEncoding(StandardCharsets.UTF_8.name());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.getWriter().write(objectMapper.writeValueAsString(
			ApiResponse.error(
				new ApiError(ErrorCode.FORBIDDEN.code(), "인증 요청 헤더가 올바르지 않습니다.", null),
				RequestTrace.traceId(request)
			)
		));
	}
}
