package com.medi.application.hospital.result;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record HospitalAllowStatusBulkUpdateResult(
	@JsonProperty("updated_count") int updatedCount,
	@JsonProperty("allow_status") String allowStatus,
	List<Long> ids
) {
}
