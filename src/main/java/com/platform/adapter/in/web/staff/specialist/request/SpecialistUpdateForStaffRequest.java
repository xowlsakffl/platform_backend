package com.platform.adapter.in.web.staff.specialist.request;

import com.platform.common.web.multipart.MultipartMediaFileSource;
import com.platform.application.specialist.command.UpdateSpecialistForStaffCommand;
import com.platform.domain.specialist.SpecialistAllowStatus;
import com.platform.domain.specialist.SpecialistField;
import com.platform.domain.specialist.SpecialistScheduleMode;
import com.platform.domain.specialist.SpecialistStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.web.bind.annotation.BindParam;
import org.springframework.web.multipart.MultipartFile;

public record SpecialistUpdateForStaffRequest(
	@Size(max = 255) String name,
	@Size(max = 20) String gender,
	@Size(max = 50) String position,
	@BindParam("career_started_at") @PastOrPresent LocalDate careerStartedAt,
	@BindParam("specialist_field") SpecialistField specialistField,
	@Size(max = 500) String introduction,
	@BindParam("schedule_mode") SpecialistScheduleMode scheduleMode,
	@BindParam("operation_hours") Object operationHours,
	@BindParam("holiday_policy") Object holidayPolicy,
	SpecialistStatus status,
	@BindParam("allow_status") SpecialistAllowStatus allowStatus,
	@Size(max = 500) String reason,
	@BindParam("option_assignments") @Size(max = 100_000) String optionAssignments,
	@BindParam("profile_image_files") @Size(max = 3) List<MultipartFile> profileImageFiles,
	@BindParam("profile_image_order[]") @Size(max = 3) List<String> profileImageOrder,
	@BindParam("certification_image_files") @Size(max = 5) List<MultipartFile> certificationImageFiles,
	@BindParam("certification_image_order[]") @Size(max = 5) List<String> certificationImageOrder
) {

	public UpdateSpecialistForStaffCommand toCommand(Set<String> requestFields) {
		Set<String> fields = normalizeFields(requestFields);
		return new UpdateSpecialistForStaffCommand(
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
			allowStatus,
			reason,
			optionAssignments,
			MultipartMediaFileSource.from(profileImageFiles),
			cleanOrder(profileImageOrder),
			MultipartMediaFileSource.from(certificationImageFiles),
			cleanOrder(certificationImageOrder),
			fields
		);
	}

	private Set<String> normalizeFields(Set<String> requestFields) {
		Set<String> fields = new LinkedHashSet<>();
		for (String field : requestFields) {
			fields.add(field.endsWith("[]") ? field.substring(0, field.length() - 2) : field);
		}
		if (profileImageFiles != null && profileImageFiles.stream().anyMatch(file -> !file.isEmpty())) {
			fields.add("profile_image_files");
		}
		if (certificationImageFiles != null && certificationImageFiles.stream().anyMatch(file -> !file.isEmpty())) {
			fields.add("certification_image_files");
		}
		return Set.copyOf(fields);
	}

	private List<String> cleanOrder(List<String> order) {
		return order == null ? List.of() : order.stream().filter(org.springframework.util.StringUtils::hasText).toList();
	}
}
