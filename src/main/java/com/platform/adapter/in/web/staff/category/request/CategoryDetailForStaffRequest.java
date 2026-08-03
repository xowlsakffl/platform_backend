package com.platform.adapter.in.web.staff.category.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CategoryDetailForStaffRequest(
	@Size(max = 2) List<@Pattern(regexp = "^(parent|children)$") String> include
) {

	public List<String> normalizedInclude() {
		return include == null ? List.of() : include.stream().distinct().toList();
	}
}
