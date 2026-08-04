package com.platform.infrastructure.mail;

import com.platform.application.partner.PartnerAccountInvitationMailSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.partner-account-invitation", name = "mail-mode", havingValue = "log")
public class LoggingPartnerAccountInvitationMailSender implements PartnerAccountInvitationMailSender {

	private static final Logger log = LoggerFactory.getLogger(LoggingPartnerAccountInvitationMailSender.class);

	@Override
	public void send(
		String recipient,
		String recipientName,
		String partnerName,
		String setupUrl,
		long expireHours
	) {
		log.info(
			"Local partner account invitation recipient={}, recipientName={}, partner={}, expires={}h, url={}",
			recipient,
			recipientName,
			partnerName,
			expireHours,
			setupUrl
		);
	}
}
