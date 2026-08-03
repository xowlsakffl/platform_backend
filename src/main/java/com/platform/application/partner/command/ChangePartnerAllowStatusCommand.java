package com.platform.application.partner.command;

import com.platform.domain.partner.PartnerAllowStatus;
import java.util.List;

public record ChangePartnerAllowStatusCommand(
	List<Long> ids,
	PartnerAllowStatus allowStatus,
	String reason
) {
}
