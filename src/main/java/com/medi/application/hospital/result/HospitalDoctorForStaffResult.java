package com.medi.application.hospital.result;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.medi.application.doctor.result.DoctorSpecialistResult;
import java.time.LocalDateTime;

public record HospitalDoctorForStaffResult(
	Long id,
	@JsonProperty("hospital_id") Long hospitalId,
	String name,
	String position,
	DoctorSpecialistResult specialist,
	@JsonProperty("sort_order") int sortOrder,
	@JsonProperty("allow_status") String allowStatus,
	String status,
	@JsonProperty("created_at") LocalDateTime createdAt,
	@JsonProperty("updated_at") LocalDateTime updatedAt
) {
}
