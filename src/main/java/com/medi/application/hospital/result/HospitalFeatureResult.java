package com.medi.application.hospital.result;

public record HospitalFeatureResult(
	Long id,
	String code,
	String name,
	int sortOrder,
	String status
) {
}
