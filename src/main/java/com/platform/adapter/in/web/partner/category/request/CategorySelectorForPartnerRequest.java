package com.platform.adapter.in.web.partner.category.request;

import com.platform.domain.category.CategoryUsageType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.BindParam;

public record CategorySelectorForPartnerRequest(
	@NotNull CategoryUsageType usage,
	@BindParam("parent_id") @Positive Long parentId,
	@Size(max = 100) String q
) {
}
