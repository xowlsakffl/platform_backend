package com.platform.application.partner.result;

public record IssuedPartnerAccountInvitation(
	Long invitationId,
	Long partnerId,
	String partnerName,
	String email,
	String recipientName,
	String rawToken,
	String tokenHash
) {
}
