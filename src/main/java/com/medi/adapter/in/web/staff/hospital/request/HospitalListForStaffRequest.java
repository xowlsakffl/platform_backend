package com.medi.adapter.in.web.staff.hospital.request;

import com.medi.application.hospital.query.SearchHospitalsQuery;
import com.medi.common.error.ApiException;
import com.medi.common.error.ErrorCode;
import com.medi.domain.account.AccountHospitalStatus;
import com.medi.domain.hospital.HospitalAllowStatus;
import com.medi.domain.hospital.HospitalStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import org.springframework.web.bind.annotation.BindParam;

public record HospitalListForStaffRequest(
	@Size(max = 100) String q,
	List<HospitalStatus> status,
	@BindParam("account_status")
	List<AccountHospitalStatus> accountStatus,
	@BindParam("allow_status")
	List<HospitalAllowStatus> allowStatus,
	@BindParam("category_ids")
	@Size(min = 1, max = 5) List<@Positive Long> categoryIds,
	@Size(max = 2) List<@Pattern(regexp = "categories|features") String> include,
	Boolean dormant,
	@BindParam("start_date")
	String startDate,
	@BindParam("end_date")
	String endDate,
	@BindParam("updated_start_date")
	String updatedStartDate,
	@BindParam("updated_end_date")
	String updatedEndDate,
	@Pattern(regexp = "id|name|view_count|allow_status|status|created_at|updated_at|last_login_at|evaluation_count|evaluation_average_rating")
	String sort,
	@Pattern(regexp = "asc|desc") String direction,
	@Min(1)
	Integer page,
	@BindParam("per_page")
	@Min(1)
	@Max(100)
	Integer perPage
) {

	public SearchHospitalsQuery toQuery() {
		validateDateRange(startDate, endDate);
		validateDateRange(updatedStartDate, updatedEndDate);
		return new SearchHospitalsQuery(
			q,
			status,
			accountStatus,
			allowStatus,
			categoryIds,
			include,
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
