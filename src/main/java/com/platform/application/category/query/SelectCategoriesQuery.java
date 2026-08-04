package com.platform.application.category.query;

import com.platform.domain.category.CategoryDomain;
import com.platform.domain.category.CategoryGroup;
import com.platform.domain.category.CategoryStatus;
import com.platform.domain.category.CategoryUsageType;
import java.util.List;

public record SelectCategoriesQuery(
	CategoryDomain domain,
	CategoryUsageType usage,
	String q,
	Long parentId,
	String parentCode,
	Integer depth,
	CategoryGroup groupCode,
	List<CategoryStatus> status,
	Boolean menuVisible,
	String sort,
	String direction,
	int perPage
) {
}
