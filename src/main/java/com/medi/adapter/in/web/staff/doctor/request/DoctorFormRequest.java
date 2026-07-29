package com.medi.adapter.in.web.staff.doctor.request;

import com.medi.adapter.in.web.support.MultipartMediaFileSource;
import com.medi.application.doctor.command.SaveDoctorCommand;
import com.medi.domain.doctor.DoctorAllowStatus;
import com.medi.domain.doctor.DoctorSpecialistField;
import com.medi.domain.doctor.DoctorStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import org.springframework.web.bind.annotation.BindParam;
import org.springframework.web.multipart.MultipartFile;

public record DoctorFormRequest(
	@BindParam("hospital_id") @NotNull @Positive Long hospitalId,
	@BindParam("sort_order") @Min(0) @Max(100_000) Integer sortOrder,
	@NotBlank @Size(max = 100) String name,
	@NotBlank @Pattern(regexp = "^(남|여)$") String gender,
	@NotBlank @Size(max = 50) String position,
	@BindParam("career_started_at") LocalDate careerStartedAt,
	@BindParam("license_number") @NotBlank @Size(max = 100) String licenseNumber,
	@BindParam("specialist_field") @NotNull DoctorSpecialistField specialistField,
	@NotNull DoctorStatus status,
	@BindParam("allow_status") @NotNull DoctorAllowStatus allowStatus,
	@BindParam("category_ids[]") @NotEmpty @Size(max = 5) List<@Positive Long> categoryIds,
	@NotBlank @Size(max = 10_000) String educations,
	@NotBlank @Size(max = 10_000) String careers,
	@BindParam("etc_contents") @NotBlank @Size(max = 10_000) String etcContents,
	@BindParam("profile_image") MultipartFile profileImage,
	@BindParam("existing_profile_image_id") @Positive Long existingProfileImageId,
	@BindParam("license_image") MultipartFile licenseImage,
	@BindParam("existing_license_image_id") @Positive Long existingLicenseImageId,
	@BindParam("specialist_certificate_image") MultipartFile specialistCertificateImage,
	@BindParam("existing_specialist_certificate_image_id") @Positive Long existingSpecialistCertificateImageId
) {

	public SaveDoctorCommand toCommand() {
		return new SaveDoctorCommand(
			hospitalId,
			sortOrder,
			name,
			gender,
			position,
			careerStartedAt,
			licenseNumber,
			specialistField,
			status,
			allowStatus,
			categoryIds,
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
