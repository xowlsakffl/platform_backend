package com.platform.application.auth;

import com.platform.domain.account.AccountActorType;

public interface PasswordResetRateLimitPolicy {

	void checkLinkRequest(AccountActorType actorType, String email, String ipAddress);

	void checkTokenVerification(AccountActorType actorType, String email, String ipAddress);

	void checkPasswordReset(AccountActorType actorType, String email, String ipAddress);
}
