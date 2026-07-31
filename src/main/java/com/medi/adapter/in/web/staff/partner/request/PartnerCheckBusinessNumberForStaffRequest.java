package com.medi.adapter.in.web.staff.partner.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public record PartnerCheckBusinessNumberForStaffRequest(
	@JsonProperty("business_number") @NotBlank String businessNumber
) {
}
