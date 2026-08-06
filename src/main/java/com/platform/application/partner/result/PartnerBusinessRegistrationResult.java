package com.platform.application.partner.result;

import com.platform.application.media.result.MediaResult;
import com.fasterxml.jackson.annotation.JsonProperty;

public record PartnerBusinessRegistrationResult(
	Long id,
	@JsonProperty("business_number") String businessNumber,
	@JsonProperty("company_name") String companyName,
	@JsonProperty("ceo_name") String ceoName,
	@JsonProperty("business_type") String businessType,
	@JsonProperty("business_item") String businessItem,
	@JsonProperty("business_address") String businessAddress,
	@JsonProperty("business_address_detail") String businessAddressDetail,
	@JsonProperty("settlement_account") PartnerSettlementAccountResult settlementAccount,
	String status,
	@JsonProperty("certificate_media") MediaResult certificateMedia
) {
}
