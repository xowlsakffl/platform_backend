package com.platform.application.partner.result;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PartnerAccountInvitationResult(
	Long id,
	@JsonProperty("partner_id") Long partnerId,
	String email,
	String status,
	@JsonProperty("delivery_status") String deliveryStatus,
	@JsonProperty("expires_at") LocalDateTime expiresAt,
	@JsonProperty("sent_at") LocalDateTime sentAt,
	@JsonProperty("accepted_at") LocalDateTime acceptedAt,
	@JsonProperty("canceled_at") LocalDateTime canceledAt,
	@JsonProperty("created_by_staff_id") Long createdByStaffId,
	@JsonProperty("created_at") LocalDateTime createdAt,
	@JsonProperty("updated_at") LocalDateTime updatedAt
) {
}
