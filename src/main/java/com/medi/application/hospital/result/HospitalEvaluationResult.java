package com.medi.application.hospital.result;

import com.fasterxml.jackson.annotation.JsonProperty;

public record HospitalEvaluationResult(
	int count,
	@JsonProperty("average_rating") double averageRating
) {
}
