package com.medi.application.partner.command;

import com.medi.domain.partner.PartnerAllowStatus;
import java.util.List;

public record ChangePartnerAllowStatusCommand(
	List<Long> ids,
	PartnerAllowStatus allowStatus,
	String reason
) {
}
