package com.platform.application.partner;

public interface PartnerAccountInvitationMailSender {

	void send(
		String recipient,
		String partnerName,
		String setupUrl,
		long expireHours
	);
}
