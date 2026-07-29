package com.medi.adapter.in.web.staff.category.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.medi.application.category.command.CreateCategoryCommand;
import com.medi.domain.category.CategoryDomain;
import com.medi.domain.category.CategoryGroup;
import com.medi.domain.category.CategoryStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CategoryCreateForStaffRequest(
	@NotNull CategoryDomain domain,
	@NotBlank @Size(max = 120) String name,
	@JsonProperty("parent_id") Long parentId,
	@Size(max = 80) String code,
	@JsonProperty("group_code") CategoryGroup groupCode,
	@JsonProperty("sort_order") @Min(0) Integer sortOrder,
	CategoryStatus status,
	@JsonProperty("is_menu_visible") Boolean menuVisible
) {

	public CreateCategoryCommand toCommand() {
		return new CreateCategoryCommand(
			domain,
			name,
			parentId,
			code,
			groupCode,
			sortOrder,
			status,
			menuVisible,
			null
		);
	}
}
