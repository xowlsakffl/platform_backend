package com.platform.application.auth;

import com.platform.domain.account.AccountActorType;

public interface PasswordResetMailSender {

	void send(AccountActorType actorType, String recipient, String resetUrl, long expireMinutes);
}
