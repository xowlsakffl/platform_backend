package com.platform.adapter.in.web.partner.specialist.request;

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

public record SpecialistCreateForPartnerRequest(
	@BindParam("sort_order") @Min(0) Integer sortOrder,
	@NotBlank @Size(max = 255) String name,
	@Size(max = 20) String gender,
	@Size(max = 50) String position,
	@BindParam("career_started_at") LocalDate careerStartedAt,
	@BindParam("license_number") @Size(max = 100) @Pattern(regexp = "^\\d*$") String licenseNumber,
	@BindParam("specialist_field") @NotNull SpecialistField specialistField,
	SpecialistStatus status,
	@Size(max = 10_000) String educations,
	@Size(max = 10_000) String careers,
	@BindParam("etc_contents") @Size(max = 10_000) String etcContents,
	@BindParam("profile_image") MultipartFile profileImage,
	@BindParam("license_image") MultipartFile licenseImage,
	@BindParam("specialist_certificate_image") MultipartFile specialistCertificateImage
) {

	public SaveSpecialistCommand toCommand() {
		return new SaveSpecialistCommand(
			null,
			sortOrder,
			name,
			gender,
			position,
			careerStartedAt,
			licenseNumber,
			specialistField,
			status,
			SpecialistAllowStatus.PENDING,
			educations,
			careers,
			etcContents,
			MultipartMediaFileSource.from(profileImage),
			null,
			MultipartMediaFileSource.from(licenseImage),
			null,
			MultipartMediaFileSource.from(specialistCertificateImage),
			null
		);
	}
}
