package com.platform.application.partner.command;

public record AcceptPartnerAccountInvitationCommand(
	String token,
	String name,
	String nickname,
	String phone,
	String password
) {
}
