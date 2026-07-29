package com.medi.application.category.command;

import com.medi.application.media.storage.MediaFileSource;
import com.medi.domain.category.CategoryGroup;
import com.medi.domain.category.CategoryStatus;

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
