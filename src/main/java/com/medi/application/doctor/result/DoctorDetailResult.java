package com.medi.application.doctor.result;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record DoctorDetailResult(
	Long id,
	@JsonProperty("hospital_id") Long hospitalId,
	@JsonProperty("hospital_name") String hospitalName,
	@JsonProperty("hospital_business_number") String hospitalBusinessNumber,
	String name,
	String gender,
	String position,
	@JsonProperty("career_started_at") LocalDate careerStartedAt,
	@JsonProperty("license_number") String licenseNumber,
	DoctorSpecialistResult specialist,
	String status,
	@JsonProperty("status_label") String statusLabel,
	@JsonProperty("allow_status") String allowStatus,
	@JsonProperty("allow_status_label") String allowStatusLabel,
	List<String> educations,
	List<String> careers,
	@JsonProperty("etc_contents") List<String> etcContents,
	List<DoctorCategoryResult> categories,
	@JsonProperty("profile_image") DoctorMediaResult profileImage,
	@JsonProperty("license_image") DoctorMediaResult licenseImage,
	@JsonProperty("specialist_certificate_image") DoctorMediaResult specialistCertificateImage,
	@JsonProperty("view_count") long viewCount,
	@JsonProperty("created_at") LocalDateTime createdAt,
	@JsonProperty("updated_at") LocalDateTime updatedAt
) {
}
