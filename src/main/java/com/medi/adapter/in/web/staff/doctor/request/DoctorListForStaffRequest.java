package com.medi.adapter.in.web.staff.doctor.request;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.medi.application.doctor.query.SearchDoctorsForStaffQuery;
import com.medi.common.error.ApiException;
import com.medi.common.error.ErrorCode;
import com.medi.domain.doctor.DoctorAllowStatus;
import com.medi.domain.doctor.DoctorSpecialistField;
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

public record DoctorListForStaffRequest(
	@BindParam("hospital_id") @Positive Long hospitalId,
	@Size(max = 100) String q,
	@BindParam("allow_status") String allowStatus,
	String position,
	@BindParam("specialist_field") String specialistField,
	@BindParam("category_ids") String categoryIds,
	@BindParam("category_id") String categoryId,
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

	public SearchDoctorsForStaffQuery toQuery() {
		Set<String> positions = parseStrings(position);
		if (positions.stream().anyMatch(value -> !Set.of("대표원장", "원장").contains(value))) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "직책 값이 올바르지 않습니다.");
		}
		Set<Long> categories = parseLongs(categoryIds == null ? categoryId : categoryIds);
		if (categories.size() > 5) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "진료분야는 최대 5개까지 선택할 수 있습니다.");
		}
		return new SearchDoctorsForStaffQuery(
			hospitalId,
			q,
			parseEnums(allowStatus, DoctorAllowStatus::valueOf, "검수 상태"),
			positions,
			parseEnums(specialistField, DoctorSpecialistField::valueOf, "전문의 분류"),
			categories,
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
