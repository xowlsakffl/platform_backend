package com.medi.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medi.common.error.ErrorCode;
import com.medi.common.web.ApiError;
import com.medi.common.web.ApiResponse;
import com.medi.common.web.RequestTrace;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

@Component
public class ApiSecurityExceptionHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

	private final ObjectMapper objectMapper;

	public ApiSecurityExceptionHandler(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	@Override
	public void commence(
		HttpServletRequest request,
		HttpServletResponse response,
		org.springframework.security.core.AuthenticationException authException
	) throws IOException, ServletException {
		write(response, ErrorCode.UNAUTHORIZED, request);
	}

	@Override
	public void handle(
		HttpServletRequest request,
		HttpServletResponse response,
		AccessDeniedException accessDeniedException
	) throws IOException, ServletException {
		write(response, ErrorCode.FORBIDDEN, request);
	}

	private void write(HttpServletResponse response, ErrorCode errorCode, HttpServletRequest request) throws IOException {
		response.setStatus(errorCode.httpStatus().value());
		response.setCharacterEncoding(StandardCharsets.UTF_8.name());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.getWriter().write(objectMapper.writeValueAsString(
			ApiResponse.error(ApiError.of(errorCode), RequestTrace.traceId(request))
		));
	}
}
