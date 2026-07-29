package com.medi.application.doctor.result;

import com.fasterxml.jackson.annotation.JsonProperty;

public record HospitalOptionForStaffResult(
	Long id,
	String name,
	@JsonProperty("business_number") String businessNumber
) {
}
