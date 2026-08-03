package com.platform.application.category.command;

import com.platform.application.media.storage.MediaFileSource;
import com.platform.domain.category.CategoryDomain;
import com.platform.domain.category.CategoryGroup;
import com.platform.domain.category.CategoryStatus;

public record CreateCategoryCommand(
	CategoryDomain domain,
	String name,
	Long parentId,
	String code,
	CategoryGroup groupCode,
	Integer sortOrder,
	CategoryStatus status,
	Boolean menuVisible,
	MediaFileSource icon
) {
}
