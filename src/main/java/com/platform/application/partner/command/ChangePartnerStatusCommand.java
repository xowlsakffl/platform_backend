package com.platform.application.partner.command;

import com.platform.domain.partner.PartnerStatus;

public record ChangePartnerStatusCommand(
	PartnerStatus status,
	String reason
) {
}
