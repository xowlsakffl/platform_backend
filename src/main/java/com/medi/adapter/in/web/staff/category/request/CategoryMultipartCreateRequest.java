package com.medi.adapter.in.web.staff.category.request;

import com.medi.adapter.in.web.support.MultipartMediaFileSource;
import com.medi.application.category.command.CreateCategoryCommand;
import com.medi.domain.category.CategoryDomain;
import com.medi.domain.category.CategoryGroup;
import com.medi.domain.category.CategoryStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.BindParam;
import org.springframework.web.multipart.MultipartFile;

public record CategoryMultipartCreateRequest(
	@NotNull CategoryDomain domain,
	@NotBlank @Size(max = 120) String name,
	@BindParam("parent_id") @Positive Long parentId,
	@Size(max = 80) String code,
	@BindParam("group_code") CategoryGroup groupCode,
	@BindParam("sort_order") @Min(0) Integer sortOrder,
	CategoryStatus status,
	@BindParam("is_menu_visible") Boolean menuVisible,
	MultipartFile icon
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
			MultipartMediaFileSource.from(icon)
		);
	}
}
