package com.platform.application.partner.result;

import com.platform.application.media.result.MediaResult;
import com.platform.application.category.result.CategoryReferenceResult;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PartnerListItemResult(
	Long id,
	String name,
	@JsonProperty("allow_status") String allowStatus,
	@JsonProperty("reviewer_staff_id") Long reviewerStaffId,
	@JsonProperty("reviewer_staff_name") String reviewerStaffName,
	@JsonProperty("review_started_at") LocalDateTime reviewStartedAt,
	String status,
	@JsonProperty("created_at") LocalDateTime createdAt,
	MediaResult logo,
	PartnerAccountResult account,
	@JsonProperty("assigned_staff") PartnerAssignedStaffResult assignedStaff,
	List<CategoryReferenceResult> categories,
	String region,
	@JsonProperty("specialist_count") long specialistCount,
	@JsonProperty("option_count") long optionCount,
	@JsonProperty("registration_source") String registrationSource,
	@JsonProperty("account_link_status") String accountLinkStatus,
	@JsonProperty("representative_email") String representativeEmail,
	@JsonProperty("invitation_email") String invitationEmail,
	@JsonProperty("invitation_sent_at") LocalDateTime invitationSentAt,
	@JsonProperty("invitation_expires_at") LocalDateTime invitationExpiresAt
) {
}
