package com.platform.common.error;

public class ApiException extends RuntimeException {

	private final ErrorCode errorCode;
	private final Object details;

	public ApiException(ErrorCode errorCode) {
		this(errorCode, errorCode.defaultMessage(), null);
	}

	public ApiException(ErrorCode errorCode, String message) {
		this(errorCode, message, null);
	}

	public ApiException(ErrorCode errorCode, String message, Object details) {
		super(message);
		this.errorCode = errorCode;
		this.details = details;
	}

	public ErrorCode errorCode() {
		return errorCode;
	}

	public Object details() {
		return details;
	}
}
