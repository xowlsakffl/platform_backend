package com.medi.adapter.in.web.staff.hospital.request;

import jakarta.validation.constraints.NotBlank;

public record HospitalCheckNameRequest(
	@NotBlank String name
) {
}
