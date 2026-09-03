package com.platform.adapter.in.web.staff.partner.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PartnerOwnerAccountUpdateForStaffRequest(
	@JsonProperty("login_id") @NotBlank @Size(max = 30) String loginId
) {
}
