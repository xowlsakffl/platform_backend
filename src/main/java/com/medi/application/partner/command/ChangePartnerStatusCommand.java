package com.medi.application.partner.command;

import com.medi.domain.partner.PartnerStatus;

public record ChangePartnerStatusCommand(
	PartnerStatus status,
	String reason
) {
}
