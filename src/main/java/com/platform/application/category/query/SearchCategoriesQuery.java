package com.platform.application.category.query;

import com.platform.domain.category.CategoryDomain;
import com.platform.domain.category.CategoryGroup;
import com.platform.domain.category.CategoryStatus;
import java.util.List;

public record SearchCategoriesQuery(
	CategoryDomain domain,
	String q,
	Long parentId,
	Integer depth,
	CategoryGroup groupCode,
	List<CategoryStatus> status,
	List<String> include,
	Boolean menuVisible,
	String sort,
	String direction,
	int page,
	int perPage
) {
}
