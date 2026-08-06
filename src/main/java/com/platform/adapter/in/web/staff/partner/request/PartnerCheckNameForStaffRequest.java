package com.platform.adapter.in.web.staff.partner.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PartnerCheckNameForStaffRequest(
	@NotBlank @Size(max = 30) String name
) {
}
