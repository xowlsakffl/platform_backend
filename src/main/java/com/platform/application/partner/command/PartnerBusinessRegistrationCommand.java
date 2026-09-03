package com.platform.application.partner.command;

import java.time.LocalDate;

public record PartnerBusinessRegistrationCommand(
	String businessNumber,
	String companyName,
	String ceoName,
	LocalDate openingDate,
	String settlementBankName,
	String settlementAccountNumber,
	String settlementAccountHolder
) {
}
