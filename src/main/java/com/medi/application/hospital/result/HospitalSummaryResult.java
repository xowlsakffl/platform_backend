package com.medi.application.hospital.result;

public record HospitalSummaryResult(
	long total,
	long pending,
	long approved,
	long rejected,
	long active,
	long suspended,
	long withdrawn
) {
}
