package com.medi.common.error;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

public enum ErrorCode {
	INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다."),
	INVALID_REQUEST(HttpStatus.UNPROCESSABLE_ENTITY, "요청 값이 올바르지 않습니다."),
	UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
	FORBIDDEN(HttpStatus.FORBIDDEN, "권한이 없습니다."),
	NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 정보를 찾을 수 없습니다."),
	METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "허용되지 않는 HTTP 메서드입니다."),
	TOKEN_ERROR(HttpStatusCode.valueOf(419), "토큰이 유효하지 않습니다."),
	DB_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "데이터베이스 오류가 발생했습니다."),
	USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
	RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS, "요청이 너무 많습니다. 잠시 후 다시 시도해주세요."),
	PAYLOAD_TOO_LARGE(HttpStatusCode.valueOf(413), "요청 용량이 초과되었습니다.");

	private final HttpStatusCode httpStatus;
	private final String defaultMessage;

	ErrorCode(HttpStatusCode httpStatus, String defaultMessage) {
		this.httpStatus = httpStatus;
		this.defaultMessage = defaultMessage;
	}

	public String code() {
		return name();
	}

	public HttpStatusCode httpStatus() {
		return httpStatus;
	}

	public String defaultMessage() {
		return defaultMessage;
	}
}
