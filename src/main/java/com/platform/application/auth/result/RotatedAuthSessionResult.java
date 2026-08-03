package com.platform.application.auth.result;

import com.platform.domain.account.AccountActorType;

public record RotatedAuthSessionResult(
	String sessionId,
	AccountActorType actorType,
	Long accountId,
	String refreshToken,
	long refreshExpiresIn,
	boolean persistent
) {
}
