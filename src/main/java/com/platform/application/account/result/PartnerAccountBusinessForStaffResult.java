package com.platform.application.account.result;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PartnerAccountBusinessForStaffResult(
	Long id,
	String name,
	String status,
	@JsonProperty("allow_status") String allowStatus
) {
}
