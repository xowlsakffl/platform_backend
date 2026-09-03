package com.platform.adapter.in.web.staff.account.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PartnerAccountPasswordResetLinkForStaffRequest(
	@NotBlank
	@Email
	@Size(max = 255)
	String email
) {
}
