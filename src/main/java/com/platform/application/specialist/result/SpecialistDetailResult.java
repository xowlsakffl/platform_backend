package com.platform.application.specialist.result;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record SpecialistDetailResult(
	Long id,
	@JsonProperty("partner_id") Long partnerId,
	@JsonProperty("partner_name") String partnerName,
	@JsonProperty("partner_business_number") String partnerBusinessNumber,
	@JsonProperty("sort_order") int sortOrder,
	String name,
	String gender,
	String position,
	@JsonProperty("career_started_at") LocalDate careerStartedAt,
	@JsonProperty("license_number") String licenseNumber,
	SpecialistFieldResult specialist,
	String status,
	@JsonProperty("status_label") String statusLabel,
	@JsonProperty("allow_status") String allowStatus,
	@JsonProperty("allow_status_label") String allowStatusLabel,
	List<String> educations,
	List<String> careers,
	@JsonProperty("etc_contents") List<String> etcContents,
	@JsonProperty("profile_image") SpecialistMediaResult profileImage,
	@JsonProperty("license_image") SpecialistMediaResult licenseImage,
	@JsonProperty("specialist_certificate_image") SpecialistMediaResult specialistCertificateImage,
	@JsonProperty("view_count") long viewCount,
	@JsonProperty("created_at") LocalDateTime createdAt,
	@JsonProperty("updated_at") LocalDateTime updatedAt
) {
}
