package com.platform.application.partner.result;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PartnerSettlementAccountResult(
	@JsonProperty("bank_name") String bankName,
	@JsonProperty("account_number") String accountNumber,
	@JsonProperty("account_holder") String accountHolder
) {
}
