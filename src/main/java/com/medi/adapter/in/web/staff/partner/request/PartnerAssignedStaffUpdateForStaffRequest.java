package com.medi.adapter.in.web.staff.partner.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Positive;

public record PartnerAssignedStaffUpdateForStaffRequest(
	@JsonProperty("assigned_staff_id") @Positive Long assignedStaffId
) {
}
