package com.medi.adapter.in.web.staff.hospital.request;

import com.medi.application.hospital.query.SearchHospitalsQuery;
import com.medi.domain.hospital.HospitalAllowStatus;
import com.medi.domain.hospital.HospitalDepartment;
import com.medi.domain.hospital.HospitalStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import org.springframework.web.bind.annotation.BindParam;

public record HospitalListRequest(
	String q,
	List<HospitalStatus> status,
	@BindParam("allow_status")
	List<HospitalAllowStatus> allowStatus,
	List<HospitalDepartment> department,
	@BindParam("category_ids")
	List<Long> categoryIds,
	Boolean dormant,
	@BindParam("start_date")
	String startDate,
	@BindParam("end_date")
	String endDate,
	@BindParam("updated_start_date")
	String updatedStartDate,
	@BindParam("updated_end_date")
	String updatedEndDate,
	String sort,
	String direction,
	@Min(1)
	Integer page,
	@BindParam("per_page")
	@Min(1)
	@Max(100)
	Integer perPage
) {

	public SearchHospitalsQuery toQuery() {
		return new SearchHospitalsQuery(
			q,
			status,
			allowStatus,
			department,
			categoryIds,
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
}
