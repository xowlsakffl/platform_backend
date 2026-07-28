package com.medi.application.hospital.result;

public record HospitalCategoryResult(
	Long id,
	String name,
	String fullPath,
	int depth,
	int sortOrder
) {
}
