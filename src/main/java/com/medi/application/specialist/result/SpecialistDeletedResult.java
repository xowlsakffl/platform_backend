package com.medi.application.specialist.result;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

public record SpecialistDeletedResult(
	@JsonProperty("deleted_id") Long deletedId,
	@JsonProperty("deleted_at") LocalDateTime deletedAt
) {
}
