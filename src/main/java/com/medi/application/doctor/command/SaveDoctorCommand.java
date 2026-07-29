package com.medi.application.doctor.command;

import com.medi.application.media.storage.MediaFileSource;
import com.medi.domain.doctor.DoctorAllowStatus;
import com.medi.domain.doctor.DoctorSpecialistField;
import com.medi.domain.doctor.DoctorStatus;
import java.time.LocalDate;
import java.util.List;

public record SaveDoctorCommand(
	Long hospitalId,
	Integer sortOrder,
	String name,
	String gender,
	String position,
	LocalDate careerStartedAt,
	String licenseNumber,
	DoctorSpecialistField specialistField,
	DoctorStatus status,
	DoctorAllowStatus allowStatus,
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
