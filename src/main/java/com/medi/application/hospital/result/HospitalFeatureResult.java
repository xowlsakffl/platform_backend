package com.medi.application.hospital.result;

import com.fasterxml.jackson.annotation.JsonProperty;

public record HospitalFeatureResult(
	Long id,
	String code,
	String name,
	@JsonProperty("sort_order") int sortOrder,
	String status
) {
}
