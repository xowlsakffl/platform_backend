package com.platform.application.partner.command;

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
	String settlementAccountHolder
) {
}
