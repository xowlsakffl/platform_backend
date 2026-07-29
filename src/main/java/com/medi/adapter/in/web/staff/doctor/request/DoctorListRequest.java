package com.medi.adapter.in.web.staff.doctor.request;

import com.medi.application.doctor.query.SearchDoctorsQuery;
import com.medi.common.error.ApiException;
import com.medi.common.error.ErrorCode;
import com.medi.domain.doctor.DoctorAllowStatus;
import com.medi.domain.doctor.DoctorSpecialistField;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Function;
import org.springframework.web.bind.annotation.BindParam;

public record DoctorListRequest(
	@Size(max = 100) String q,
	@BindParam("allow_status") String allowStatus,
	String position,
	@BindParam("specialist_field") String specialistField,
	@BindParam("category_ids") String categoryIds,
	@Pattern(regexp = "^(career_years|review_count|consultation_count)?$") String metric,
	@BindParam("metric_min") @Min(0) Integer metricMin,
	@BindParam("metric_max") @Min(0) Integer metricMax,
	@BindParam("start_date") String startDate,
	@BindParam("end_date") String endDate,
	@Pattern(regexp = "^(id|name|gender|position|specialist_field|allow_status|career_years|review_count|consultation_count|created_at)?$") String sort,
	@Pattern(regexp = "^(asc|desc)?$") String direction,
	@Min(1) Integer page,
	@BindParam("per_page") @Min(1) @Max(100) Integer perPage
) {

	public SearchDoctorsQuery toQuery() {
		return new SearchDoctorsQuery(
			q,
			parseEnums(allowStatus, DoctorAllowStatus::valueOf, "검수 상태"),
			parseStrings(position),
			parseEnums(specialistField, DoctorSpecialistField::valueOf, "전문의 분류"),
			parseLongs(categoryIds),
			blankToNull(metric),
			metricMin,
			metricMax,
			blankToNull(startDate),
			blankToNull(endDate),
			blankToNull(sort),
			blankToNull(direction) == null ? "desc" : direction,
			page == null ? 1 : page,
			perPage == null ? 10 : perPage
		);
	}

	private <T> Set<T> parseEnums(String value, Function<String, T> parser, String fieldName) {
		Set<T> result = new LinkedHashSet<>();
		for (String item : split(value)) {
			try {
				result.add(parser.apply(item));
			} catch (IllegalArgumentException exception) {
				throw new ApiException(ErrorCode.INVALID_REQUEST, fieldName + " 값이 올바르지 않습니다.");
			}
		}
		return result;
	}

	private Set<String> parseStrings(String value) {
		return new LinkedHashSet<>(split(value));
	}

	private Set<Long> parseLongs(String value) {
		Set<Long> result = new LinkedHashSet<>();
		for (String item : split(value)) {
			try {
				long id = Long.parseLong(item);
				if (id <= 0) {
					throw new NumberFormatException();
				}
				result.add(id);
			} catch (NumberFormatException exception) {
				throw new ApiException(ErrorCode.INVALID_REQUEST, "카테고리 ID가 올바르지 않습니다.");
			}
		}
		return result;
	}

	private java.util.List<String> split(String value) {
		if (value == null || value.isBlank()) {
			return java.util.List.of();
		}
		return Arrays.stream(value.split("[,|]"))
			.map(String::trim)
			.filter(item -> !item.isEmpty())
			.toList();
	}

	private String blankToNull(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}
}
