package com.platform.application.partner.command;

public record AcceptPartnerAccountInvitationCommand(
	String token,
	String loginId,
	String phone,
	String password
) {
}
