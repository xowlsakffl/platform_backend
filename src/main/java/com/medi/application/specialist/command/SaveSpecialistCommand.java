package com.medi.application.specialist.command;

import com.medi.application.media.storage.MediaFileSource;
import com.medi.domain.specialist.SpecialistAllowStatus;
import com.medi.domain.specialist.SpecialistField;
import com.medi.domain.specialist.SpecialistStatus;
import java.time.LocalDate;
import java.util.List;

public record SaveSpecialistCommand(
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
	List<Long> categoryIds,
	String educations,
	String careers,
	String etcContents,
	MediaFileSource profileImage,
	Long existingProfileImageId,
	MediaFileSource licenseImage,
	Long existingLicenseImageId,
	MediaFileSource specialistCertificateImage,
	Long existingSpecialistCertificateImageId
) {
}
