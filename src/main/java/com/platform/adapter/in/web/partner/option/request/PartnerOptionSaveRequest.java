package com.platform.adapter.in.web.partner.option.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.platform.application.partner.command.SavePartnerOptionCommand;
import com.platform.domain.partner.PartnerPriceType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

public record PartnerOptionSaveRequest(
	@JsonProperty("category_id") @NotNull @Positive Long categoryId,
	@NotBlank @Size(max = 120) String name,
	@Size(max = 1000) String description,
	@DecimalMin("0") @Digits(integer = 10, fraction = 2) BigDecimal price,
	@JsonProperty("price_type") @NotNull PartnerPriceType priceType,
	@JsonProperty("duration_minutes") @Positive @Max(1440) Integer durationMinutes,
	@JsonProperty("is_visible") @NotNull Boolean visible,
	@JsonProperty("sort_order") @Min(0) int sortOrder,
	@Size(max = 100) List<@Valid SpecialistPriceRequest> specialists
) {

	public SavePartnerOptionCommand toCommand() {
		return new SavePartnerOptionCommand(
			categoryId,
			name,
			description,
			price,
			priceType,
			durationMinutes,
			visible,
			sortOrder,
			specialists == null
				? List.of()
				: specialists.stream().map(SpecialistPriceRequest::toCommand).toList()
		);
	}

	public record SpecialistPriceRequest(
		@JsonProperty("specialist_id") @NotNull @Positive Long specialistId,
		@JsonProperty("price_override")
		@DecimalMin("0") @Digits(integer = 10, fraction = 2) BigDecimal priceOverride,
		@JsonProperty("price_type_override") PartnerPriceType priceTypeOverride
	) {

		private SavePartnerOptionCommand.SpecialistPriceCommand toCommand() {
			return new SavePartnerOptionCommand.SpecialistPriceCommand(
				specialistId,
				priceOverride,
				priceTypeOverride
			);
		}
	}
}
