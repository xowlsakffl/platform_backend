package com.platform.common.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestTraceFilter extends OncePerRequestFilter {

	public static final String HEADER_NAME = "X-Request-Id";
	public static final String ATTRIBUTE_NAME = RequestTraceFilter.class.getName() + ".TRACE_ID";
	private static final int MAX_TRACE_ID_LENGTH = 100;

	@Override
	protected void doFilterInternal(
		HttpServletRequest request,
		HttpServletResponse response,
		FilterChain filterChain
	) throws ServletException, IOException {
		String traceId = resolveTraceId(request);

		request.setAttribute(ATTRIBUTE_NAME, traceId);
		response.setHeader(HEADER_NAME, traceId);
		MDC.put("traceId", traceId);

		try {
			filterChain.doFilter(request, response);
		} finally {
			MDC.remove("traceId");
		}
	}

	private String resolveTraceId(HttpServletRequest request) {
		String requestId = request.getHeader(HEADER_NAME);

		if (requestId == null || requestId.isBlank()) {
			return UUID.randomUUID().toString();
		}

		String normalized = requestId.trim();

		if (normalized.length() > MAX_TRACE_ID_LENGTH) {
			return UUID.randomUUID().toString();
		}

		return normalized;
	}
}
