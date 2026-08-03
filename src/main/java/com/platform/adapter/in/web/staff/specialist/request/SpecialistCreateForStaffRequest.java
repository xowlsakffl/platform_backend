package com.platform.adapter.in.web.staff.specialist.request;

import com.platform.common.web.multipart.MultipartMediaFileSource;
import com.platform.application.specialist.command.SaveSpecialistCommand;
import com.platform.domain.specialist.SpecialistAllowStatus;
import com.platform.domain.specialist.SpecialistField;
import com.platform.domain.specialist.SpecialistStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import org.springframework.web.bind.annotation.BindParam;
import org.springframework.web.multipart.MultipartFile;

public record SpecialistCreateForStaffRequest(
	@BindParam("partner_id") @NotNull @Positive Long partnerId,
	@BindParam("sort_order") @Min(0) Integer sortOrder,
	@NotBlank @Size(max = 255) String name,
	@Size(max = 20) String gender,
	@Size(max = 50) String position,
	@BindParam("career_started_at") LocalDate careerStartedAt,
	@BindParam("license_number") @Size(max = 100) @Pattern(regexp = "^\\d*$") String licenseNumber,
	@BindParam("specialist_field") @NotNull SpecialistField specialistField,
	SpecialistStatus status,
	@BindParam("allow_status") SpecialistAllowStatus allowStatus,
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

	public SaveSpecialistCommand toCommand() {
		return new SaveSpecialistCommand(
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
			educations,
			careers,
			etcContents,
			MultipartMediaFileSource.from(profileImage),
			existingProfileImageId,
			MultipartMediaFileSource.from(licenseImage),
			existingLicenseImageId,
			MultipartMediaFileSource.from(specialistCertificateImage),
			existingSpecialistCertificateImageId
		);
	}
}
