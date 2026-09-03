package com.platform.application.account.result;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.List;

public record PartnerAccountListItemForStaffResult(
	Long id,
	String name,
	@JsonProperty("login_id") String loginId,
	String email,
	String phone,
	String status,
	@JsonProperty("last_login_at") LocalDateTime lastLoginAt,
	@JsonProperty("managed_partner_count") int managedPartnerCount,
	@JsonProperty("managed_partners") List<PartnerAccountBusinessForStaffResult> managedPartners,
	@JsonProperty("created_at") LocalDateTime createdAt,
	@JsonProperty("updated_at") LocalDateTime updatedAt
) {
}
