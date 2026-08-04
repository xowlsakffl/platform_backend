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
	String status,
	@JsonProperty("created_at") LocalDateTime createdAt,
	MediaResult logo,
	PartnerAccountResult account,
	@JsonProperty("assigned_staff") PartnerAssignedStaffResult assignedStaff,
	List<CategoryReferenceResult> categories,
	@JsonProperty("registration_source") String registrationSource,
	@JsonProperty("account_link_status") String accountLinkStatus,
	@JsonProperty("invitation_email") String invitationEmail,
	@JsonProperty("invitation_sent_at") LocalDateTime invitationSentAt,
	@JsonProperty("invitation_expires_at") LocalDateTime invitationExpiresAt
) {
}
