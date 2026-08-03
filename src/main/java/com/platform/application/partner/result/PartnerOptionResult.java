package com.platform.application.partner.result;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record PartnerOptionResult(
	Long id,
	String name,
	String description,
	BigDecimal price,
	@JsonProperty("price_type") String priceType,
	@JsonProperty("duration_minutes") Integer durationMinutes,
	@JsonProperty("is_visible") boolean visible,
	@JsonProperty("sort_order") int sortOrder,
	List<SpecialistPriceResult> specialists,
	@JsonProperty("created_at") LocalDateTime createdAt,
	@JsonProperty("updated_at") LocalDateTime updatedAt
) {

	public record SpecialistPriceResult(
		@JsonProperty("specialist_id") Long specialistId,
		@JsonProperty("specialist_name") String specialistName,
		@JsonProperty("price_override") BigDecimal priceOverride,
		@JsonProperty("price_type_override") String priceTypeOverride,
		@JsonProperty("effective_price") BigDecimal effectivePrice,
		@JsonProperty("effective_price_type") String effectivePriceType
	) {
	}
}
