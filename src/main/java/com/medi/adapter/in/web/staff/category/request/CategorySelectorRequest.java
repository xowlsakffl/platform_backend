package com.medi.adapter.in.web.staff.category.request;

import com.medi.application.category.query.SelectCategoriesQuery;
import com.medi.domain.category.CategoryDomain;
import com.medi.domain.category.CategoryGroup;
import com.medi.domain.category.CategoryStatus;
import com.medi.domain.category.CategoryUsageType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.web.bind.annotation.BindParam;

public record CategorySelectorRequest(
	@NotNull CategoryDomain domain,
	CategoryUsageType usage,
	@Size(max = 100) String q,
	@BindParam("parent_id") Long parentId,
	@BindParam("parent_code") @Size(max = 80) String parentCode,
	@Min(1) @Max(4) Integer depth,
	@BindParam("group_code") CategoryGroup groupCode,
	List<CategoryStatus> status,
	@BindParam("is_menu_visible") Boolean menuVisible,
	@Pattern(regexp = "^(id|name|sort_order|depth|group_code|status)$") String sort,
	@Pattern(regexp = "^(asc|desc)$") String direction,
	@BindParam("per_page") @Min(1) @Max(100) Integer perPage
) {

	public SelectCategoriesQuery toQuery() {
		return new SelectCategoriesQuery(
			domain,
			usage,
			q,
			parentId,
			parentCode,
			depth,
			groupCode,
			status,
			menuVisible,
			sort == null ? "sort_order" : sort,
			direction == null ? "asc" : direction,
			perPage == null ? 50 : perPage
		);
	}
}
