package com.medi.application.doctor.result;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

public record DoctorDeletedResult(
	@JsonProperty("deleted_id") Long deletedId,
	@JsonProperty("deleted_at") LocalDateTime deletedAt
) {
}
