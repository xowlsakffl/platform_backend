package com.platform.common.web;

import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;

public final class RequestTrace {

	private RequestTrace() {
	}

	public static String traceId(HttpServletRequest request) {
		Object traceId = request.getAttribute(RequestTraceFilter.ATTRIBUTE_NAME);

		if (traceId instanceof String value && !value.isBlank()) {
			return value;
		}

		return UUID.randomUUID().toString();
	}
}
