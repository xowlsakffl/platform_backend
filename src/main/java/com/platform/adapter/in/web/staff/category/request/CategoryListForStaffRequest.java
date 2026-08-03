package com.platform.adapter.in.web.staff.category.request;

import com.platform.application.category.query.SearchCategoriesQuery;
import com.platform.domain.category.CategoryDomain;
import com.platform.domain.category.CategoryGroup;
import com.platform.domain.category.CategoryStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.web.bind.annotation.BindParam;

public record CategoryListForStaffRequest(
	@NotNull CategoryDomain domain,
	@Size(max = 100) String q,
	@BindParam("parent_id") Long parentId,
	@Min(1) @Max(4) Integer depth,
	@BindParam("group_code") CategoryGroup groupCode,
	List<CategoryStatus> status,
	@Size(max = 2) List<@Pattern(regexp = "^(parent|children)$") String> include,
	@BindParam("is_menu_visible") Boolean menuVisible,
	@Pattern(regexp = "^(id|name|sort_order|depth|group_code|status|created_at|updated_at)$") String sort,
	@Pattern(regexp = "^(asc|desc)$") String direction,
	@Min(1) Integer page,
	@BindParam("per_page") @Min(1) @Max(100) Integer perPage
) {

	public SearchCategoriesQuery toQuery() {
		return new SearchCategoriesQuery(
			domain,
			q,
			parentId,
			depth,
			groupCode,
			status,
			include == null ? List.of() : include.stream().distinct().toList(),
			menuVisible,
			sort == null ? "sort_order" : sort,
			direction == null ? "asc" : direction,
			page == null ? 1 : page,
			perPage == null ? 50 : perPage
		);
	}
}
