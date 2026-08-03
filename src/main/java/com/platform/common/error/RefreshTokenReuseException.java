package com.platform.common.error;

public class RefreshTokenReuseException extends ApiException {

	public RefreshTokenReuseException() {
		super(ErrorCode.TOKEN_ERROR, "리프레시 토큰 재사용이 감지되어 인증 세션을 종료했습니다.");
	}
}
