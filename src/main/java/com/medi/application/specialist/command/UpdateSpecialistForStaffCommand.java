package com.medi.application.specialist.command;

import com.medi.application.media.storage.MediaFileSource;
import com.medi.domain.specialist.SpecialistAllowStatus;
import com.medi.domain.specialist.SpecialistField;
import com.medi.domain.specialist.SpecialistStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

public record UpdateSpecialistForStaffCommand(
	Long partnerId,
	Integer sortOrder,
	String name,
	String gender,
	String position,
	LocalDate careerStartedAt,
	String licenseNumber,
	SpecialistField specialistField,
	SpecialistStatus status,
	SpecialistAllowStatus allowStatus,
	String reason,
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
