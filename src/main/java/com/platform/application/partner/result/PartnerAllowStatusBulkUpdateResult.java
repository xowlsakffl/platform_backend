package com.platform.application.partner.result;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record PartnerAllowStatusBulkUpdateResult(
	@JsonProperty("updated_count") int updatedCount,
	@JsonProperty("allow_status") String allowStatus,
	List<Long> ids
) {
}
