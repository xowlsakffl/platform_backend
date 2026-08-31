package com.platform.application.specialist.result;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record SpecialistListItemResult(
	Long id,
	@JsonProperty("partner_id") Long partnerId,
	@JsonProperty("partner_name") String partnerName,
	@JsonProperty("sort_order") int sortOrder,
	String name,
	String gender,
	String position,
	String introduction,
	@JsonProperty("specialist_field") SpecialistFieldResult specialistField,
	@JsonProperty("career_started_at") LocalDate careerStartedAt,
	String status,
	@JsonProperty("status_label") String statusLabel,
	@JsonProperty("allow_status") String allowStatus,
	@JsonProperty("allow_status_label") String allowStatusLabel,
	@JsonProperty("schedule_mode") String scheduleMode,
	@JsonProperty("schedule_mode_label") String scheduleModeLabel,
	@JsonProperty("option_count") long optionCount,
	@JsonProperty("profile_image") SpecialistMediaResult profileImage,
	@JsonProperty("created_at") LocalDateTime createdAt,
	@JsonProperty("updated_at") LocalDateTime updatedAt
) {
}
