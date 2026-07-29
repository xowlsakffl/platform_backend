package com.medi.adapter.in.web.staff.hospital.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public record HospitalCheckBusinessNumberForStaffRequest(
	@JsonProperty("business_number") @NotBlank String businessNumber
) {
}
