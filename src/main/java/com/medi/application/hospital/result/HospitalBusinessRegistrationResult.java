package com.medi.application.hospital.result;

import java.time.LocalDate;

public record HospitalBusinessRegistrationResult(
	Long id,
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
	LocalDate issuedAt,
	String status
) {
}
