package com.platform.application.partner.result;

import com.platform.application.media.result.MediaResult;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;

public record PartnerBusinessRegistrationResult(
	Long id,
	@JsonProperty("business_number") String businessNumber,
	@JsonProperty("company_name") String companyName,
	@JsonProperty("ceo_name") String ceoName,
	@JsonProperty("opening_date") LocalDate openingDate,
	@JsonProperty("settlement_account") PartnerSettlementAccountResult settlementAccount,
	String status,
	@JsonProperty("certificate_media") MediaResult certificateMedia
) {
}
