package com.medi.adapter.in.web.staff.specialist.request;

import com.medi.common.web.multipart.MultipartMediaFileSource;
import com.medi.application.specialist.command.UpdateSpecialistForStaffCommand;
import com.medi.domain.specialist.SpecialistAllowStatus;
import com.medi.domain.specialist.SpecialistField;
import com.medi.domain.specialist.SpecialistStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.web.bind.annotation.BindParam;
import org.springframework.web.multipart.MultipartFile;

public record SpecialistUpdateForStaffRequest(
	@BindParam("partner_id") @Positive Long partnerId,
	@BindParam("sort_order") @Min(0) Integer sortOrder,
	@Size(max = 255) String name,
	@Size(max = 20) String gender,
	@Size(max = 50) String position,
	@BindParam("career_started_at") LocalDate careerStartedAt,
	@BindParam("license_number") @Size(max = 100) @Pattern(regexp = "^\\d*$") String licenseNumber,
	@BindParam("specialist_field") SpecialistField specialistField,
	SpecialistStatus status,
	@BindParam("allow_status") SpecialistAllowStatus allowStatus,
	@Size(max = 500) String reason,
	@BindParam("category_ids[]") @Size(max = 5) List<@Positive Long> categoryIds,
	@Size(max = 10_000) String educations,
	@Size(max = 10_000) String careers,
	@BindParam("etc_contents") @Size(max = 10_000) String etcContents,
	@BindParam("profile_image") MultipartFile profileImage,
	@BindParam("existing_profile_image_id") @Positive Long existingProfileImageId,
	@BindParam("license_image") MultipartFile licenseImage,
	@BindParam("existing_license_image_id") @Positive Long existingLicenseImageId,
	@BindParam("specialist_certificate_image") MultipartFile specialistCertificateImage,
	@BindParam("existing_specialist_certificate_image_id") @Positive Long existingSpecialistCertificateImageId
) {

	public UpdateSpecialistForStaffCommand toCommand(Set<String> requestFields) {
		Set<String> fields = normalizeFields(requestFields);
		return new UpdateSpecialistForStaffCommand(
			partnerId,
			sortOrder,
			name,
			gender,
			position,
			careerStartedAt,
			licenseNumber,
			specialistField,
			status,
			allowStatus,
			reason,
			categoryIds,
			educations,
			careers,
			etcContents,
			MultipartMediaFileSource.from(profileImage),
			existingProfileImageId,
			MultipartMediaFileSource.from(licenseImage),
			existingLicenseImageId,
			MultipartMediaFileSource.from(specialistCertificateImage),
			existingSpecialistCertificateImageId,
			fields
		);
	}

	private Set<String> normalizeFields(Set<String> requestFields) {
		Set<String> fields = new LinkedHashSet<>();
		for (String field : requestFields) {
			fields.add(field.endsWith("[]") ? field.substring(0, field.length() - 2) : field);
		}
		if (profileImage != null && !profileImage.isEmpty()) {
			fields.add("profile_image");
		}
		if (licenseImage != null && !licenseImage.isEmpty()) {
			fields.add("license_image");
		}
		if (specialistCertificateImage != null && !specialistCertificateImage.isEmpty()) {
			fields.add("specialist_certificate_image");
		}
		return Set.copyOf(fields);
	}
}
