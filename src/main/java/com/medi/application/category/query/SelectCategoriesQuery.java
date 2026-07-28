package com.medi.application.category.query;

import com.medi.domain.category.CategoryDomain;
import com.medi.domain.category.CategoryGroup;
import com.medi.domain.category.CategoryStatus;
import com.medi.domain.category.CategoryUsageType;
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
