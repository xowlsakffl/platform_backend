package com.platform.application.partner.result;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PartnerAccountInvitationAcceptedResult(
	@JsonProperty("partner_id") Long partnerId,
	@JsonProperty("partner_name") String partnerName,
	String email,
	@JsonProperty("allow_status") String allowStatus,
	String message
) {
}
