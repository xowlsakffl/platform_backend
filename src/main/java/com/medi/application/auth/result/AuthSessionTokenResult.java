package com.medi.application.auth.result;

public record AuthSessionTokenResult(
	AuthTokenResult token,
	String refreshToken,
	long refreshExpiresIn,
	boolean persistent
) {
}
