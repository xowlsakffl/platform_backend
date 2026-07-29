package com.medi.application.hospital.result;

import com.medi.application.media.result.MediaResult;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;

public record HospitalBusinessRegistrationResult(
	Long id,
	@JsonProperty("business_number") String businessNumber,
	@JsonProperty("company_name") String companyName,
	@JsonProperty("ceo_name") String ceoName,
	@JsonProperty("business_type") String businessType,
	@JsonProperty("business_item") String businessItem,
	@JsonProperty("business_address") String businessAddress,
	@JsonProperty("business_address_detail") String businessAddressDetail,
	@JsonProperty("settlement_account") HospitalSettlementAccountResult settlementAccount,
	@JsonProperty("issued_at") LocalDate issuedAt,
	String status,
	@JsonProperty("certificate_media") MediaResult certificateMedia
) {
}
