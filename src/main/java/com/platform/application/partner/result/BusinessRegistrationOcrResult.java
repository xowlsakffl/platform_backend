package com.platform.application.partner.result;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

public record BusinessRegistrationOcrResult(
	@JsonProperty("business_number") String businessNumber,
	@JsonProperty("company_name") String companyName,
	@JsonProperty("ceo_name") String ceoName,
	@JsonProperty("business_address") String businessAddress,
	@JsonProperty("opening_date") String openingDate,
	Map<String, Double> confidences,
	@JsonProperty("requires_confirmation") boolean requiresConfirmation,
	@JsonProperty("already_registered") boolean alreadyRegistered
) {

	public BusinessRegistrationOcrResult withAlreadyRegistered(boolean value) {
		return new BusinessRegistrationOcrResult(
			businessNumber,
			companyName,
			ceoName,
			businessAddress,
			openingDate,
			confidences,
			requiresConfirmation,
			value
		);
	}
}
