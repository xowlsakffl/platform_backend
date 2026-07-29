package com.medi.application.doctor.result;

import com.fasterxml.jackson.annotation.JsonProperty;

public record DoctorCategoryResult(
	Long id,
	String domain,
	String name,
	@JsonProperty("full_path") String fullPath,
	@JsonProperty("is_primary") boolean primary
) {
}
