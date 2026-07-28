package com.medi.application.hospital.result;

import java.time.LocalDateTime;
import java.util.List;

public record HospitalDetailResult(
	Long id,
	String name,
	String department,
	String departmentLabel,
	String description,
	String youtubeLink,
	String address,
	String addressDetail,
	String latitude,
	String longitude,
	HospitalContactGroupResult contacts,
	List<HospitalContactResult> contactItems,
	String consultingHours,
	Object operationHours,
	String direction,
	long viewCount,
	int evaluationCount,
	double evaluationAverageRating,
	String allowStatus,
	String allowStatusLabel,
	String status,
	String statusLabel,
	LocalDateTime createdAt,
	LocalDateTime updatedAt,
	HospitalBusinessRegistrationResult businessRegistration,
	HospitalAccountResult accountHospital,
	List<HospitalCategoryResult> categories,
	List<HospitalFeatureResult> features
) {
}
