package com.medi.application.partner.result;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PartnerFeatureResult(
	Long id,
	String code,
	String name,
	@JsonProperty("sort_order") int sortOrder,
	String status
) {
}
