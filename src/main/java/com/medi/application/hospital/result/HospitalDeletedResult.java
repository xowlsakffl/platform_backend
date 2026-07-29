package com.medi.application.hospital.result;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

public record HospitalDeletedResult(
	@JsonProperty("deleted_id") Long deletedId,
	@JsonProperty("deleted_at") LocalDateTime deletedAt
) {
}
