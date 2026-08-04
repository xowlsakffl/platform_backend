package com.platform.application.partner.result;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

public record PartnerAccountInvitationVerifyResult(
	boolean valid,
	@JsonProperty("partner_name") String partnerName,
	@JsonProperty("masked_email") String maskedEmail,
	@JsonProperty("expires_at") LocalDateTime expiresAt
) {
}
