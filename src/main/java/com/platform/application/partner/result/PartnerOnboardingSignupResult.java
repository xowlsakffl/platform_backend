package com.platform.application.partner.result;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PartnerOnboardingSignupResult(
	@JsonProperty("partner_id") Long partnerId,
	@JsonProperty("account_partner_id") Long accountPartnerId,
	String email,
	@JsonProperty("allow_status") String allowStatus
) {
}
