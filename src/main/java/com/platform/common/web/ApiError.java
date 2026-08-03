package com.platform.common.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.platform.common.error.ErrorCode;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(String code, String message, Object details) {

	public static ApiError of(ErrorCode errorCode) {
		return of(errorCode, errorCode.defaultMessage(), null);
	}

	public static ApiError of(ErrorCode errorCode, String message) {
		return of(errorCode, message, null);
	}

	public static ApiError of(ErrorCode errorCode, Object details) {
		return of(errorCode, errorCode.defaultMessage(), details);
	}

	public static ApiError of(ErrorCode errorCode, String message, Object details) {
		return new ApiError(errorCode.code(), message, details);
	}
}
