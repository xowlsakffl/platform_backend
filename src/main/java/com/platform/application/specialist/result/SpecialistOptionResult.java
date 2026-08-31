package com.platform.application.specialist.result;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

public record SpecialistOptionResult(
	Long id,
	@JsonProperty("partner_option_id") Long partnerOptionId,
	String name,
	String description,
	@JsonProperty("regular_price") BigDecimal regularPrice,
	@JsonProperty("sale_price") BigDecimal salePrice,
	@JsonProperty("duration_minutes") Integer durationMinutes,
	@JsonProperty("is_visible") boolean visible,
	@JsonProperty("regular_price_override") BigDecimal regularPriceOverride,
	@JsonProperty("sale_price_override") BigDecimal salePriceOverride,
	@JsonProperty("effective_regular_price") BigDecimal effectiveRegularPrice,
	@JsonProperty("effective_sale_price") BigDecimal effectiveSalePrice,
	@JsonProperty("effective_price") BigDecimal effectivePrice,
	@JsonProperty("discount_rate") Integer discountRate
) {
}
