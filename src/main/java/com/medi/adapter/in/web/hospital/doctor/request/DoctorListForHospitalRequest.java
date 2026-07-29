package com.medi.adapter.in.web.hospital.doctor.request;

import com.medi.application.doctor.query.SearchDoctorsForHospitalQuery;
import com.medi.common.error.ApiException;
import com.medi.common.error.ErrorCode;
import com.medi.domain.doctor.DoctorAllowStatus;
import com.medi.domain.doctor.DoctorStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Function;
import org.springframework.web.bind.annotation.BindParam;

public record DoctorListForHospitalRequest(
	@Size(max = 100) String q,
	String status,
	@BindParam("allow_status") String allowStatus,
	@Pattern(regexp = "^(id|name|position|status|allow_status|sort_order|created_at)?$") String sort,
	@Pattern(regexp = "^(asc|desc)?$") String direction,
	@Min(1) Integer page,
	@BindParam("per_page") @Min(1) @Max(100) Integer perPage
) {

	public SearchDoctorsForHospitalQuery toQuery() {
		return new SearchDoctorsForHospitalQuery(
			blankToNull(q),
			parseEnums(status, DoctorStatus::valueOf, "운영 상태"),
			parseEnums(allowStatus, DoctorAllowStatus::valueOf, "검수 상태"),
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
