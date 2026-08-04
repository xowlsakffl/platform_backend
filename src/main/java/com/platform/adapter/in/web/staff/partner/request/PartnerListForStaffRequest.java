package com.platform.adapter.in.web.staff.partner.request;

import com.platform.application.partner.query.SearchPartnersQuery;
import com.platform.common.error.ApiException;
import com.platform.common.error.ErrorCode;
import com.platform.domain.account.AccountPartnerStatus;
import com.platform.domain.partner.PartnerAllowStatus;
import com.platform.domain.partner.PartnerStatus;
import com.platform.domain.partner.PartnerRegistrationSource;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import org.springframework.web.bind.annotation.BindParam;

public record PartnerListForStaffRequest(
	@Size(max = 100) String q,
	List<PartnerStatus> status,
	@BindParam("account_status")
	List<AccountPartnerStatus> accountStatus,
	@BindParam("allow_status")
	List<PartnerAllowStatus> allowStatus,
	@BindParam("category_ids") List<@Min(1) Long> categoryIds,
	@BindParam("registration_source")
	List<PartnerRegistrationSource> registrationSources,
	Boolean dormant,
	@BindParam("start_date")
	String startDate,
	@BindParam("end_date")
	String endDate,
	@BindParam("updated_start_date")
	String updatedStartDate,
	@BindParam("updated_end_date")
	String updatedEndDate,
	@Pattern(regexp = "id|name|region|allow_status|status|created_at")
	String sort,
	@Pattern(regexp = "asc|desc") String direction,
	@Min(1)
	Integer page,
	@BindParam("per_page")
	@Min(1)
	@Max(100)
	Integer perPage
) {

	public SearchPartnersQuery toQuery() {
		validateDateRange(startDate, endDate);
		validateDateRange(updatedStartDate, updatedEndDate);
		return new SearchPartnersQuery(
			q,
			status,
			accountStatus,
			allowStatus,
			categoryIds,
			registrationSources,
			dormant,
			startDate,
			endDate,
			updatedStartDate,
			updatedEndDate,
			sort,
			normalizeDirection(),
			pageOrDefault(),
			perPageOrDefault()
		);
	}

	private String normalizeDirection() {
		if (direction == null || direction.isBlank()) {
			return "desc";
		}
		return direction;
	}

	private int pageOrDefault() {
		if (page == null) {
			return 1;
		}
		return page;
	}

	private int perPageOrDefault() {
		if (perPage == null) {
			return 15;
		}
		return perPage;
	}

	private void validateDateRange(String start, String end) {
		try {
			LocalDate startDateValue = start == null || start.isBlank() ? null : LocalDate.parse(start);
			LocalDate endDateValue = end == null || end.isBlank() ? null : LocalDate.parse(end);
			if (startDateValue != null && endDateValue != null && endDateValue.isBefore(startDateValue)) {
				throw new ApiException(ErrorCode.INVALID_REQUEST, "종료일은 시작일보다 빠를 수 없습니다.");
			}
		} catch (DateTimeParseException exception) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "날짜 형식은 yyyy-MM-dd 이어야 합니다.");
		}
	}
}
