package com.medi.adapter.in.web.staff.partner.request;

import jakarta.validation.constraints.NotBlank;

public record PartnerCheckNameForStaffRequest(
	@NotBlank String name
) {
}
