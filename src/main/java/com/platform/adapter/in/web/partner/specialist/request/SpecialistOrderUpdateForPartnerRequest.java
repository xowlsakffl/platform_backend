package com.platform.adapter.in.web.partner.specialist.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.platform.application.specialist.command.ReorderSpecialistsForStaffCommand;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;

public record SpecialistOrderUpdateForPartnerRequest(
	@JsonProperty("specialist_ids")
	@NotEmpty
	@Size(max = 500)
	List<@NotNull @Positive Long> specialistIds
) {
	public ReorderSpecialistsForStaffCommand toCommand() {
		return new ReorderSpecialistsForStaffCommand(List.copyOf(specialistIds));
	}
}
