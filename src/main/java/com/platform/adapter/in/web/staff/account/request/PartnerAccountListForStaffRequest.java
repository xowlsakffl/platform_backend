package com.platform.adapter.in.web.staff.account.request;

import com.platform.application.account.query.SearchPartnerAccountsForStaffQuery;
import com.platform.common.error.ApiException;
import com.platform.common.error.ErrorCode;
import com.platform.domain.account.AccountPartnerStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import org.springframework.web.bind.annotation.BindParam;

public record PartnerAccountListForStaffRequest(
	@Size(max = 100) String q,
	List<AccountPartnerStatus> status,
	Boolean dormant,
	@BindParam("start_date") String startDate,
	@BindParam("end_date") String endDate,
	@Pattern(regexp = "id|name|login_id|status|last_login_at|created_at") String sort,
	@Pattern(regexp = "asc|desc") String direction,
	@Min(1) Integer page,
	@BindParam("per_page") @Min(1) @Max(100) Integer perPage
) {

	public SearchPartnerAccountsForStaffQuery toQuery() {
		validateDateRange(startDate, endDate);
		return new SearchPartnerAccountsForStaffQuery(
			q,
			status,
			dormant,
			startDate,
			endDate,
			sort,
			direction == null || direction.isBlank() ? "desc" : direction,
			page == null ? 1 : page,
			perPage == null ? 15 : perPage
		);
	}

	private void validateDateRange(String start, String end) {
		try {
			LocalDate startValue = start == null || start.isBlank() ? null : LocalDate.parse(start);
			LocalDate endValue = end == null || end.isBlank() ? null : LocalDate.parse(end);
			if (startValue != null && endValue != null && endValue.isBefore(startValue)) {
				throw new ApiException(ErrorCode.INVALID_REQUEST, "종료일은 시작일보다 빠를 수 없습니다.");
			}
		} catch (DateTimeParseException exception) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "날짜 형식은 yyyy-MM-dd 이어야 합니다.");
		}
	}
}
