package com.medi.common.web;

public sealed interface ApiResponse permits ApiResponse.Success, ApiResponse.Failure {

	boolean success();

	String traceId();

	static <T> Success<T> success(T data, String traceId) {
		return success(data, null, traceId);
	}

	static <T> Success<T> success(T data, Object meta, String traceId) {
		return new Success<>(true, data, meta, traceId);
	}

	static Failure error(ApiError error, String traceId) {
		return new Failure(false, error, traceId);
	}

	record Success<T>(boolean success, T data, Object meta, String traceId) implements ApiResponse {
	}

	record Failure(boolean success, ApiError error, String traceId) implements ApiResponse {
	}
}
