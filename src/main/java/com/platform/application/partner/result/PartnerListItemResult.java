package com.platform.application.partner.result;

import com.platform.application.media.result.MediaResult;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PartnerListItemResult(
	Long id,
	String name,
	@JsonProperty("allow_status") String allowStatus,
	String status,
	@JsonProperty("created_at") LocalDateTime createdAt,
	MediaResult logo,
	PartnerAccountResult account,
	@JsonProperty("assigned_staff") PartnerAssignedStaffResult assignedStaff,
	String industry
) {
}
