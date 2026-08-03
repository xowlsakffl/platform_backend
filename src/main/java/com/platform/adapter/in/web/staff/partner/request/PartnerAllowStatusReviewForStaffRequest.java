package com.platform.adapter.in.web.staff.partner.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.platform.domain.partner.PartnerAllowStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PartnerAllowStatusReviewForStaffRequest(
	@JsonProperty("allow_status") @NotNull PartnerAllowStatus allowStatus,
	@Size(max = 500) String reason
) {
}
