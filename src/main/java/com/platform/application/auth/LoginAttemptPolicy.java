package com.platform.application.auth;

import com.platform.application.auth.result.LoginAttemptStatusResult;
import com.platform.domain.account.AccountActorType;

public interface LoginAttemptPolicy {

	void assertAllowed(AccountActorType actorType, String identifier, String ipAddress);

	void recordFailure(AccountActorType actorType, String identifier, String ipAddress);

	void recordSuccess(AccountActorType actorType, String identifier, String ipAddress);

	LoginAttemptStatusResult status(AccountActorType actorType, String identifier);

	LoginAttemptStatusResult clear(AccountActorType actorType, String identifier);
}
