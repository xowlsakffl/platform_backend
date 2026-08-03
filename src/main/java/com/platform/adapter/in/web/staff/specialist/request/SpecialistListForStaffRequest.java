package com.platform.adapter.in.web.staff.specialist.request;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.application.specialist.query.SearchSpecialistsForStaffQuery;
import com.platform.common.error.ApiException;
import com.platform.common.error.ErrorCode;
import com.platform.domain.specialist.SpecialistAllowStatus;
import com.platform.domain.specialist.SpecialistField;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Function;
import org.springframework.web.bind.annotation.BindParam;

public record SpecialistListForStaffRequest(
	@BindParam("partner_id") @Positive Long partnerId,
	@Size(max = 100) String q,
	@BindParam("allow_status") String allowStatus,
	String position,
	@BindParam("specialist_field") String specialistField,
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
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
	private static final TypeReference<java.util.List<Object>> OBJECT_LIST_TYPE = new TypeReference<>() {
	};

	public SearchSpecialistsForStaffQuery toQuery() {
		Set<String> positions = parseStrings(position);
		return new SearchSpecialistsForStaffQuery(
			partnerId,
			q,
			parseEnums(allowStatus, SpecialistAllowStatus::valueOf, "검수 상태"),
			positions,
			parseEnums(specialistField, SpecialistField::valueOf, "스페셜리스트 분야"),
			blankToNull(metric),
			metricMin,
			metricMax,
			blankToNull(startDate),
			blankToNull(endDate),
			blankToNull(sort),
			blankToNull(direction) == null ? "desc" : direction,
			page == null ? 1 : page,
			perPage == null ? 15 : perPage
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

	private java.util.List<String> split(String value) {
		if (value == null || value.isBlank()) {
			return java.util.List.of();
		}
		String trimmed = value.trim();
		if (trimmed.startsWith("[")) {
			try {
				return OBJECT_MAPPER.readValue(trimmed, OBJECT_LIST_TYPE).stream()
					.filter(item -> item instanceof String || item instanceof Number)
					.map(String::valueOf)
					.map(String::trim)
					.filter(item -> !item.isEmpty())
					.distinct()
					.toList();
			} catch (JsonProcessingException exception) {
				throw new ApiException(ErrorCode.INVALID_REQUEST, "목록 검색 조건 JSON이 올바르지 않습니다.");
			}
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
