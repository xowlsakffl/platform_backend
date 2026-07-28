package com.medi.application.hospital.result;

import java.time.LocalDateTime;
import java.util.List;

public record HospitalListItemResult(
	Long id,
	String name,
	String department,
	String departmentLabel,
	String representativePhone,
	long viewCount,
	long eventCount,
	int evaluationCount,
	double evaluationAverageRating,
	long surgeryReviewCount,
	long treatmentReviewCount,
	String allowStatus,
	String allowStatusLabel,
	String status,
	String statusLabel,
	LocalDateTime createdAt,
	LocalDateTime updatedAt,
	HospitalAccountResult account,
	List<HospitalCategoryResult> categories,
	List<HospitalFeatureResult> features
) {
}
