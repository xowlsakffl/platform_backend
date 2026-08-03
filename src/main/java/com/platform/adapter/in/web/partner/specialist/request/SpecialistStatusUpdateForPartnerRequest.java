package com.platform.adapter.in.web.partner.specialist.request;

import com.platform.domain.specialist.SpecialistStatus;
import jakarta.validation.constraints.NotNull;

public record SpecialistStatusUpdateForPartnerRequest(
	@NotNull SpecialistStatus status
) {
}
