package com.platform.application.specialist.result;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record SpecialistOrderResult(
	@JsonProperty("specialist_ids") List<Long> specialistIds
) {
}
