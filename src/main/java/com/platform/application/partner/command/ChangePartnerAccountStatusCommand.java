package com.platform.application.partner.command;

import com.platform.domain.account.AccountPartnerStatus;

public record ChangePartnerAccountStatusCommand(
	AccountPartnerStatus status,
	String reason
) {
}
