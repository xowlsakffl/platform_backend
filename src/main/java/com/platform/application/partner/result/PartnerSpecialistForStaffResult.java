package com.platform.application.partner.result;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.platform.application.specialist.result.SpecialistFieldResult;
import java.time.LocalDateTime;

public record PartnerSpecialistForStaffResult(
	Long id,
	@JsonProperty("partner_id") Long partnerId,
	String name,
	String position,
	@JsonProperty("specialist_field") SpecialistFieldResult specialistField,
	@JsonProperty("sort_order") int sortOrder,
	@JsonProperty("allow_status") String allowStatus,
	String status,
	@JsonProperty("created_at") LocalDateTime createdAt,
	@JsonProperty("updated_at") LocalDateTime updatedAt
) {
}
