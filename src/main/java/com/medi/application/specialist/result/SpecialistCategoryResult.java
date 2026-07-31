package com.medi.application.specialist.result;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SpecialistCategoryResult(
	Long id,
	String domain,
	String name,
	@JsonProperty("full_path") String fullPath,
	@JsonProperty("is_primary") boolean primary
) {
}
