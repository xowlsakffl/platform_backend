package com.medi.application.hospital.result;

public record HospitalContactResult(
	Long id,
	String type,
	String value,
	int sortOrder,
	boolean primary,
	boolean active
) {
}
