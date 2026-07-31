package com.medi.application.partner.command;

import com.medi.domain.account.AccountPartnerStatus;

public record ChangePartnerAccountStatusCommand(
	AccountPartnerStatus status,
	String reason
) {
}
