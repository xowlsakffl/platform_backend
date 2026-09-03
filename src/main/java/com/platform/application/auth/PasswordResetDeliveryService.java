package com.platform.application.auth;

import com.platform.common.security.PasswordResetProperties;
import com.platform.domain.account.AccountActorType;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class PasswordResetDeliveryService {

	private final PasswordResetTokenService tokens;
	private final PasswordResetMailSender mailSender;
	private final PasswordResetProperties properties;

	public PasswordResetDeliveryService(
		PasswordResetTokenService tokens,
		PasswordResetMailSender mailSender,
		PasswordResetProperties properties
	) {
		this.tokens = tokens;
		this.mailSender = mailSender;
		this.properties = properties;
	}

	// Token replacement is committed only after the mail sender succeeds.
	@Transactional
	public boolean send(AccountActorType actorType, String accountEmail, String recipientEmail) {
		var issued = tokens.issue(actorType, accountEmail);
		if (issued.isEmpty()) {
			return false;
		}
		String resetUrl = UriComponentsBuilder.fromUriString(properties.resetUrl(actorType))
			.queryParam("actor", actorType.name().toLowerCase(Locale.ROOT))
			.queryParam("email", accountEmail)
			.queryParam("token", issued.get().rawToken())
			.build().encode().toUriString();
		mailSender.send(actorType, recipientEmail, resetUrl, properties.tokenTtlSeconds() / 60);
		return true;
	}
}
