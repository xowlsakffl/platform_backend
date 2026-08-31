package com.platform.adapter.in.web.partner.specialist.request;

import com.platform.common.web.multipart.MultipartMediaFileSource;
import com.platform.application.specialist.command.SaveSpecialistCommand;
import com.platform.domain.specialist.SpecialistAllowStatus;
import com.platform.domain.specialist.SpecialistField;
import com.platform.domain.specialist.SpecialistScheduleMode;
import com.platform.domain.specialist.SpecialistStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import org.springframework.web.bind.annotation.BindParam;
import org.springframework.web.multipart.MultipartFile;

public record SpecialistCreateForPartnerRequest(
	@NotBlank @Size(max = 255) String name,
	@Size(max = 20) String gender,
	@Size(max = 50) String position,
	@BindParam("career_started_at") @PastOrPresent LocalDate careerStartedAt,
	@BindParam("specialist_field") @NotNull SpecialistField specialistField,
	@Size(max = 500) String introduction,
	@BindParam("schedule_mode") @NotNull SpecialistScheduleMode scheduleMode,
	@BindParam("operation_hours") Object operationHours,
	@BindParam("holiday_policy") @NotNull Object holidayPolicy,
	SpecialistStatus status,
	@BindParam("option_assignments") @Size(max = 100_000) String optionAssignments,
	@BindParam("profile_image_files") @Size(max = 3) List<MultipartFile> profileImageFiles,
	@BindParam("profile_image_order[]") @Size(max = 3) List<String> profileImageOrder,
	@BindParam("certification_image_files") @Size(max = 5) List<MultipartFile> certificationImageFiles,
	@BindParam("certification_image_order[]") @Size(max = 5) List<String> certificationImageOrder
) {

	public SaveSpecialistCommand toCommand() {
		return new SaveSpecialistCommand(
			null,
			name,
			gender,
			position,
			careerStartedAt,
			specialistField,
			introduction,
			scheduleMode,
			operationHours,
			holidayPolicy,
			status,
			SpecialistAllowStatus.REVIEW_REQUESTED,
			optionAssignments,
			MultipartMediaFileSource.from(profileImageFiles),
			cleanOrder(profileImageOrder),
			MultipartMediaFileSource.from(certificationImageFiles),
			cleanOrder(certificationImageOrder)
		);
	}

	private List<String> cleanOrder(List<String> order) {
		return order == null ? List.of() : order.stream().filter(org.springframework.util.StringUtils::hasText).toList();
	}
}
