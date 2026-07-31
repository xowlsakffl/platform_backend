package com.medi.adapter.in.web.partner.specialist.request;

import com.medi.domain.specialist.SpecialistStatus;
import jakarta.validation.constraints.NotNull;

public record SpecialistStatusUpdateForPartnerRequest(
	@NotNull SpecialistStatus status
) {
}
