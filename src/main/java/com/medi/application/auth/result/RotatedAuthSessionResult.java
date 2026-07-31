package com.medi.application.auth.result;

import com.medi.domain.account.AccountActorType;

public record RotatedAuthSessionResult(
	String sessionId,
	AccountActorType actorType,
	Long accountId,
	String refreshToken,
	long refreshExpiresIn,
	boolean persistent
) {
}
