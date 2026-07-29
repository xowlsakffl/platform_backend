package com.medi.adapter.in.web.staff.category.request;

import com.medi.common.web.multipart.MultipartMediaFileSource;
import com.medi.application.category.command.UpdateCategoryCommand;
import com.medi.domain.category.CategoryGroup;
import com.medi.domain.category.CategoryStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.BindParam;
import org.springframework.web.multipart.MultipartFile;

public record CategoryUpdateMultipartForStaffRequest(
	@Size(min = 1, max = 120) String name,
	@Size(max = 80) String code,
	@BindParam("group_code") CategoryGroup groupCode,
	@BindParam("sort_order") @Min(0) Integer sortOrder,
	CategoryStatus status,
	@BindParam("is_menu_visible") Boolean menuVisible,
	MultipartFile icon
) {

	public UpdateCategoryCommand toCommand(boolean codeSpecified, boolean groupCodeSpecified) {
		return new UpdateCategoryCommand(
			name,
			code,
			codeSpecified,
			groupCode,
			groupCodeSpecified,
			sortOrder,
			status,
			menuVisible,
			MultipartMediaFileSource.from(icon)
		);
	}
}
