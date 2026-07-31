package com.medi.application.partner.result;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PartnerContactResult(
	Long id,
	String type,
	String value,
	@JsonProperty("sort_order") int sortOrder,
	@JsonProperty("is_primary") boolean primary,
	@JsonProperty("is_active") boolean active
) {
}
