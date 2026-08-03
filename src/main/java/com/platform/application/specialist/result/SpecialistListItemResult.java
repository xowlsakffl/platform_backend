package com.platform.application.specialist.result;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record SpecialistListItemResult(
	Long id,
	@JsonProperty("partner_id") Long partnerId,
	@JsonProperty("partner_name") String partnerName,
	String name,
	String gender,
	String position,
	SpecialistFieldResult specialist,
	@JsonProperty("career_started_at") LocalDate careerStartedAt,
	@JsonProperty("license_number") String licenseNumber,
	@JsonProperty("allow_status") String allowStatus,
	@JsonProperty("status") String status,
	@JsonProperty("review_count") long reviewCount,
	@JsonProperty("consultation_count") long consultationCount,
	@JsonProperty("created_at") LocalDateTime createdAt,
	@JsonProperty("profile_image") SpecialistMediaResult profileImage
) {
}
