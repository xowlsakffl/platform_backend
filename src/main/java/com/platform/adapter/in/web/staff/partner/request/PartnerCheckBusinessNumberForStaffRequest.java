package com.platform.adapter.in.web.staff.partner.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record PartnerCheckBusinessNumberForStaffRequest(
	@JsonProperty("business_number") @NotBlank
	@Pattern(regexp = PartnerRequestSupport.BUSINESS_NUMBER_PATTERN) String businessNumber
) {
}
