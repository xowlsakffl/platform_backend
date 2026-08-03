package com.platform.application.specialist.command;

import com.platform.application.media.storage.MediaFileSource;
import com.platform.domain.specialist.SpecialistField;
import com.platform.domain.specialist.SpecialistStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

public record UpdateSpecialistForPartnerCommand(
	Integer sortOrder,
	String name,
	String gender,
	String position,
	LocalDate careerStartedAt,
	String licenseNumber,
	SpecialistField specialistField,
	SpecialistStatus status,
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
