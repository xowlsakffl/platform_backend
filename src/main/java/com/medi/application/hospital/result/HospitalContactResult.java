package com.medi.application.hospital.result;

import com.fasterxml.jackson.annotation.JsonProperty;

public record HospitalContactResult(
	Long id,
	String type,
	String value,
	@JsonProperty("sort_order") int sortOrder,
	@JsonProperty("is_primary") boolean primary,
	@JsonProperty("is_active") boolean active
) {
}
