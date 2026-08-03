package com.platform.application.category.query;

import com.platform.domain.category.CategoryDomain;
import com.platform.domain.category.CategoryGroup;
import com.platform.domain.category.CategoryStatus;
import java.util.List;

public record SelectCategoriesQuery(
	CategoryDomain domain,
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
