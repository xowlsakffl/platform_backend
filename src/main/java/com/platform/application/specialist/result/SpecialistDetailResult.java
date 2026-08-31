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
	String introduction,
	@JsonProperty("career_started_at") LocalDate careerStartedAt,
	@JsonProperty("specialist_field") SpecialistFieldResult specialistField,
	String status,
	@JsonProperty("status_label") String statusLabel,
	@JsonProperty("allow_status") String allowStatus,
	@JsonProperty("allow_status_label") String allowStatusLabel,
	@JsonProperty("schedule_mode") String scheduleMode,
	@JsonProperty("schedule_mode_label") String scheduleModeLabel,
	@JsonProperty("operation_hours") Object operationHours,
	@JsonProperty("holiday_policy") Object holidayPolicy,
	@JsonProperty("reviewer_staff_id") Long reviewerStaffId,
	@JsonProperty("reviewer_staff_name") String reviewerStaffName,
	@JsonProperty("review_started_at") LocalDateTime reviewStartedAt,
	List<SpecialistOptionResult> options,
	@JsonProperty("profile_image") SpecialistMediaResult profileImage,
	@JsonProperty("profile_images") List<SpecialistMediaResult> profileImages,
	@JsonProperty("certification_images") List<SpecialistMediaResult> certificationImages,
	@JsonProperty("view_count") long viewCount,
	@JsonProperty("created_at") LocalDateTime createdAt,
	@JsonProperty("updated_at") LocalDateTime updatedAt
) {
}
