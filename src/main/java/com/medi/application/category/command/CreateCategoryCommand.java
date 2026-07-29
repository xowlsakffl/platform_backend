package com.medi.application.category.command;

import com.medi.application.media.storage.MediaFileSource;
import com.medi.domain.category.CategoryDomain;
import com.medi.domain.category.CategoryGroup;
import com.medi.domain.category.CategoryStatus;

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
