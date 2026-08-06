package com.platform.application.partner.result;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.platform.application.category.result.CategoryReferenceResult;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record PartnerOptionResult(
	Long id,
	CategoryReferenceResult category,
	String name,
	String description,
	@JsonProperty("regular_price") BigDecimal regularPrice,
	@JsonProperty("sale_price") BigDecimal salePrice,
	@JsonProperty("effective_price") BigDecimal effectivePrice,
	@JsonProperty("discount_rate") Integer discountRate,
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
		@JsonProperty("regular_price_override") BigDecimal regularPriceOverride,
		@JsonProperty("sale_price_override") BigDecimal salePriceOverride,
		@JsonProperty("effective_regular_price") BigDecimal effectiveRegularPrice,
		@JsonProperty("effective_sale_price") BigDecimal effectiveSalePrice,
		@JsonProperty("effective_price") BigDecimal effectivePrice,
		@JsonProperty("discount_rate") Integer discountRate
	) {
	}
}
