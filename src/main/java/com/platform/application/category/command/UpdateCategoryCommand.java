package com.platform.application.category.command;

import com.platform.application.media.storage.MediaFileSource;
import com.platform.domain.category.CategoryGroup;
import com.platform.domain.category.CategoryStatus;

public record UpdateCategoryCommand(
	String name,
	String code,
	boolean codeSpecified,
	CategoryGroup groupCode,
	boolean groupCodeSpecified,
	Integer sortOrder,
	CategoryStatus status,
	Boolean menuVisible,
	MediaFileSource icon
) {
}
