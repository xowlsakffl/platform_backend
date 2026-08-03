package com.platform.application.specialist.result;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PartnerOptionForStaffResult(
	Long id,
	String name,
	@JsonProperty("business_number") String businessNumber
) {
}
