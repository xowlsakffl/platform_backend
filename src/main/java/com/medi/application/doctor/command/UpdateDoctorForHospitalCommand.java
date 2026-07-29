package com.medi.application.doctor.command;

import com.medi.application.media.storage.MediaFileSource;
import com.medi.domain.doctor.DoctorSpecialistField;
import com.medi.domain.doctor.DoctorStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

public record UpdateDoctorForHospitalCommand(
	Integer sortOrder,
	String name,
	String gender,
	String position,
	LocalDate careerStartedAt,
	String licenseNumber,
	DoctorSpecialistField specialistField,
	DoctorStatus status,
	List<Long> categoryIds,
	String educations,
	String careers,
	String etcContents,
	MediaFileSource profileImage,
	Long existingProfileImageId,
	MediaFileSource licenseImage,
	Long existingLicenseImageId,
	MediaFileSource specialistCertificateImage,
	Long existingSpecialistCertificateImageId,
	Set<String> specifiedFields
) {

	public boolean specified(String field) {
		return specifiedFields.contains(field);
	}
}
