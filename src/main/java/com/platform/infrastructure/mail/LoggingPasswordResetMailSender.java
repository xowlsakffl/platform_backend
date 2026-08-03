package com.platform.infrastructure.mail;

import com.platform.application.auth.PasswordResetMailSender;
import com.platform.domain.account.AccountActorType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.auth.password-reset", name = "mail-mode", havingValue = "log")
public class LoggingPasswordResetMailSender implements PasswordResetMailSender {

	private static final Logger log = LoggerFactory.getLogger(LoggingPasswordResetMailSender.class);

	@Override
	public void send(AccountActorType actorType, String recipient, String resetUrl, long expireMinutes) {
		log.info(
			"로컬 비밀번호 재설정 메일 actor={}, recipient={}, expires={}분, url={}",
			actorType,
			recipient,
			expireMinutes,
			resetUrl
		);
	}
}
