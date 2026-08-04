package com.platform.adapter.in.web.staff.partner.request;

import com.platform.application.partner.query.SearchPartnerAccountInvitationsQuery;
import com.platform.common.error.ApiException;
import com.platform.common.error.ErrorCode;
import com.platform.domain.partner.PartnerAccountInvitationStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import org.springframework.web.bind.annotation.BindParam;

public record PartnerAccountInvitationListForStaffRequest(
	@Size(max = 100) String q,
	@BindParam("partner_id") @Min(1) Long partnerId,
	List<PartnerAccountInvitationStatus> status,
	@BindParam("start_date") String startDate,
	@BindParam("end_date") String endDate,
	@Pattern(regexp = "created_at|expires_at") String sort,
	@Pattern(regexp = "asc|desc") String direction,
	@Min(1) Integer page,
	@BindParam("per_page") @Min(1) @Max(100) Integer perPage
) {

	public SearchPartnerAccountInvitationsQuery toQuery() {
		validateDateRange();
		return new SearchPartnerAccountInvitationsQuery(
			q,
			partnerId,
			status,
			startDate,
			endDate,
			sort,
			direction == null || direction.isBlank() ? "desc" : direction,
			page == null ? 1 : page,
			perPage == null ? 20 : perPage
		);
	}

	private void validateDateRange() {
		try {
			LocalDate start = startDate == null || startDate.isBlank() ? null : LocalDate.parse(startDate);
			LocalDate end = endDate == null || endDate.isBlank() ? null : LocalDate.parse(endDate);
			if (start != null && end != null && end.isBefore(start)) {
				throw new ApiException(ErrorCode.INVALID_REQUEST, "종료일은 시작일보다 빠를 수 없습니다.");
			}
		} catch (DateTimeParseException exception) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "날짜 형식은 yyyy-MM-dd 이어야 합니다.");
		}
	}
}
