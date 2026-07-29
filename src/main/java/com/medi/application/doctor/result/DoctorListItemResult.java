package com.medi.application.doctor.result;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record DoctorListItemResult(
	Long id,
	@JsonProperty("hospital_id") Long hospitalId,
	@JsonProperty("hospital_name") String hospitalName,
	String name,
	String gender,
	String position,
	DoctorSpecialistResult specialist,
	@JsonProperty("career_started_at") LocalDate careerStartedAt,
	@JsonProperty("license_number") String licenseNumber,
	@JsonProperty("allow_status") String allowStatus,
	@JsonProperty("status") String status,
	@JsonProperty("review_count") long reviewCount,
	@JsonProperty("consultation_count") long consultationCount,
	@JsonProperty("created_at") LocalDateTime createdAt,
	@JsonProperty("profile_image") DoctorMediaResult profileImage,
	List<DoctorCategoryResult> categories
) {
}
