package com.platform.application.specialist.command;

import com.platform.application.media.storage.MediaFileSource;
import com.platform.domain.specialist.SpecialistAllowStatus;
import com.platform.domain.specialist.SpecialistField;
import com.platform.domain.specialist.SpecialistStatus;
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
