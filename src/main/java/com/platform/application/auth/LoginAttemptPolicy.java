package com.platform.application.auth;

import com.platform.domain.account.AccountActorType;

public interface LoginAttemptPolicy {

	void assertAllowed(AccountActorType actorType, String email, String ipAddress);

	void recordFailure(AccountActorType actorType, String email, String ipAddress);

	void recordSuccess(AccountActorType actorType, String email, String ipAddress);
}
