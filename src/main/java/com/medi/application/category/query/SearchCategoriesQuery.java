package com.medi.application.category.query;

import com.medi.domain.category.CategoryDomain;
import com.medi.domain.category.CategoryGroup;
import com.medi.domain.category.CategoryStatus;
import java.util.List;

public record SearchCategoriesQuery(
	CategoryDomain domain,
	String q,
	Long parentId,
	Integer depth,
	CategoryGroup groupCode,
	List<CategoryStatus> status,
	Boolean menuVisible,
	String sort,
	String direction,
	int page,
	int perPage
) {
}
