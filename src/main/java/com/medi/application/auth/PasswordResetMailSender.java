package com.medi.application.auth;

import com.medi.domain.account.AccountActorType;

public interface PasswordResetMailSender {

	void send(AccountActorType actorType, String recipient, String resetUrl, long expireMinutes);
}
