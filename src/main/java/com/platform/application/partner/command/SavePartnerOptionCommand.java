package com.platform.application.partner.command;

import java.math.BigDecimal;
import java.util.List;

public record SavePartnerOptionCommand(
	Long categoryId,
	String name,
	String description,
	BigDecimal regularPrice,
	BigDecimal salePrice,
	Integer durationMinutes,
	boolean visible,
	int sortOrder,
	List<SpecialistPriceCommand> specialists
) {

	public record SpecialistPriceCommand(
		Long specialistId,
		BigDecimal regularPriceOverride,
		BigDecimal salePriceOverride
	) {
	}
}
