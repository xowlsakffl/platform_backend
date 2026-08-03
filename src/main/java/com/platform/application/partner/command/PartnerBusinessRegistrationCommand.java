package com.platform.application.partner.command;

import java.time.LocalDate;

public record PartnerBusinessRegistrationCommand(
	String businessNumber,
	String companyName,
	String ceoName,
	String businessType,
	String businessItem,
	String businessAddress,
	String businessAddressDetail,
	String settlementBankName,
	String settlementAccountNumber,
	String settlementAccountHolder,
	String taxInvoiceEmail,
	LocalDate issuedAt
) {
}
