package com.platform.application.partner.result;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

public record PartnerAccountInvitationListItemResult(
	Long id,
	@JsonProperty("partner_id") Long partnerId,
	@JsonProperty("partner_name") String partnerName,
	String email,
	String status,
	@JsonProperty("created_at") LocalDateTime createdAt,
	@JsonProperty("sent_at") LocalDateTime sentAt,
	@JsonProperty("expires_at") LocalDateTime expiresAt,
	@JsonProperty("accepted_at") LocalDateTime acceptedAt,
	@JsonProperty("canceled_at") LocalDateTime canceledAt,
	@JsonProperty("created_by_staff_id") Long createdByStaffId,
	@JsonProperty("created_by_staff_name") String createdByStaffName,
	@JsonProperty("created_by_staff_nickname") String createdByStaffNickname
) {
}
