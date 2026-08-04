package com.platform.application.partner.command;

public record CreatePartnerAccountInvitationCommand(
	String email,
	String recipientName
) {
}
