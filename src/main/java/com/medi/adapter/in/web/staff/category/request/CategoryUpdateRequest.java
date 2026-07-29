package com.medi.adapter.in.web.staff.category.request;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.medi.application.category.command.UpdateCategoryCommand;
import com.medi.domain.category.CategoryGroup;
import com.medi.domain.category.CategoryStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public final class CategoryUpdateRequest {

	@Size(min = 1, max = 120)
	private String name;

	@Size(max = 80)
	private String code;

	private boolean codeSpecified;
	private CategoryGroup groupCode;
	private boolean groupCodeSpecified;

	@Min(0)
	private Integer sortOrder;

	private CategoryStatus status;
	private Boolean menuVisible;

	public void setName(String name) {
		this.name = name;
	}

	@JsonSetter("code")
	public void setCode(String code) {
		this.code = code;
		this.codeSpecified = true;
	}

	@JsonSetter("group_code")
	public void setGroupCode(CategoryGroup groupCode) {
		this.groupCode = groupCode;
		this.groupCodeSpecified = true;
	}

	@JsonSetter("sort_order")
	public void setSortOrder(Integer sortOrder) {
		this.sortOrder = sortOrder;
	}

	public void setStatus(CategoryStatus status) {
		this.status = status;
	}

	@JsonSetter("is_menu_visible")
	public void setMenuVisible(Boolean menuVisible) {
		this.menuVisible = menuVisible;
	}

	public UpdateCategoryCommand toCommand() {
		return new UpdateCategoryCommand(
			name,
			code,
			codeSpecified,
			groupCode,
			groupCodeSpecified,
			sortOrder,
			status,
			menuVisible,
			null
		);
	}
}
