package com.platform.application.partner.command;

import com.platform.domain.partner.PartnerPriceType;
import java.math.BigDecimal;
import java.util.List;

public record SavePartnerOptionCommand(
	String name,
	String description,
	BigDecimal price,
	PartnerPriceType priceType,
	Integer durationMinutes,
	boolean visible,
	int sortOrder,
	List<SpecialistPriceCommand> specialists
) {

	public record SpecialistPriceCommand(
		Long specialistId,
		BigDecimal priceOverride,
		PartnerPriceType priceTypeOverride
	) {
	}
}
