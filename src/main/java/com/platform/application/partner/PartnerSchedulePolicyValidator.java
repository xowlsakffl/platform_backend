package com.platform.application.partner;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.platform.common.error.ApiException;
import com.platform.common.error.ErrorCode;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class PartnerSchedulePolicyValidator {

	private static final List<String> DAYS = List.of("mon", "tue", "wed", "thu", "fri", "sat", "sun");
	private static final Set<String> REGULAR_FREQUENCIES = Set.of(
		"WEEKLY",
		"MONTHLY_FIRST",
		"MONTHLY_SECOND",
		"MONTHLY_THIRD",
		"MONTHLY_FOURTH",
		"MONTHLY_LAST"
	);
	private static final Set<String> WEEKDAYS = Set.of("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN");
	private static final Set<String> PUBLIC_HOLIDAY_CODES = Set.of(
		"NEW_YEAR",
		"LUNAR_NEW_YEAR_EVE",
		"LUNAR_NEW_YEAR",
		"LUNAR_NEW_YEAR_NEXT_DAY",
		"INDEPENDENCE_MOVEMENT_DAY",
		"LABOR_DAY",
		"CHILDRENS_DAY",
		"BUDDHAS_BIRTHDAY",
		"MEMORIAL_DAY",
		"CONSTITUTION_DAY",
		"LIBERATION_DAY",
		"CHUSEOK_EVE",
		"CHUSEOK",
		"CHUSEOK_NEXT_DAY",
		"NATIONAL_FOUNDATION_DAY",
		"HANGUL_DAY",
		"CHRISTMAS"
	);
	private static final Set<String> CUSTOM_RECURRENCES = Set.of("ONCE", "YEARLY", "MONTHLY");
	private static final int MAX_DAILY_INTERVALS = 4;
	private static final int MAX_HOLIDAY_RULES = 100;

	private final ObjectMapper objectMapper;

	public PartnerSchedulePolicyValidator(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	public String normalizeOperationHours(Object value, boolean required) {
		JsonNode root = objectNode(value, required, "운영시간");
		if (root == null) {
			return null;
		}

		String timezone = text(root, "timezone", "Asia/Seoul");
		try {
			ZoneId.of(timezone);
		} catch (DateTimeException exception) {
			throw invalid("운영시간 시간대가 올바르지 않습니다.");
		}
		((ObjectNode) root).remove("reservation_only");

		for (String day : DAYS) {
			JsonNode schedule = root.get(day);
			if (schedule == null || !schedule.isObject() || !schedule.path("is_closed").isBoolean()) {
				throw invalid("요일별 운영시간을 모두 입력해 주세요.");
			}
			validateDay(day, schedule);
		}
		return write(root, "운영시간");
	}

	public String normalizeHolidayPolicy(Object value, boolean required) {
		JsonNode root = objectNode(value, required, "휴무 정책");
		if (root == null) {
			return null;
		}
		if (!root.path("enabled").isBoolean()) {
			throw invalid("휴무일 사용 여부를 선택해 주세요.");
		}
		if (!root.path("enabled").booleanValue()) {
			return write(disabledHolidayPolicy((ObjectNode) root), "휴무 정책");
		}

		validateRegularRules(root.path("regular_rules"));
		validatePublicHolidayPolicy(root.path("public_holidays"));
		validateSubstituteHolidays(root.path("substitute_holidays"));
		validateCustomHolidays(root.path("custom_holidays"));
		return write(root, "휴무 정책");
	}

	public void assertWithinPartnerHours(String specialistHours, String partnerHours) {
		JsonNode specialist = objectNode(specialistHours, true, "전문가 운영시간");
		JsonNode partner = objectNode(partnerHours, true, "업체 운영시간");
		for (String day : DAYS) {
			JsonNode specialistDay = specialist.path(day);
			if (specialistDay.path("is_closed").asBoolean(false)) {
				continue;
			}
			List<Interval> partnerOpenings = openingIntervals(partner.path(day), day + " 업체 운영시간");
			List<Interval> specialistOpenings = openingIntervals(specialistDay, day + " 전문가 운영시간");
			if (partnerOpenings.isEmpty()
				|| specialistOpenings.stream().anyMatch(specialistOpening ->
					partnerOpenings.stream().noneMatch(partnerOpening -> partnerOpening.contains(specialistOpening)))) {
				throw invalid("전문가 운영시간은 업체 운영시간 안에서만 설정할 수 있습니다.");
			}
		}
	}

	private void validateDay(String day, JsonNode schedule) {
		boolean closed = schedule.path("is_closed").booleanValue();
		if (schedule.has("is_24_hours") && !schedule.path("is_24_hours").isBoolean()) {
			throw invalid("24시간 영업 여부가 올바르지 않습니다.");
		}
		boolean open24Hours = schedule.path("is_24_hours").asBoolean(false);

		if (schedule.has("periods")) {
			List<Interval> openingPeriods = intervals(schedule.path("periods"), day + " 영업시간");
			List<Interval> breakPeriods = intervals(schedule.path("breaks"), day + " 휴게시간");
			if (closed && (
				!openingPeriods.isEmpty()
					|| !breakPeriods.isEmpty()
					|| open24Hours
					|| schedule.hasNonNull("last_booking_time")
			)) {
				throw invalid("휴무일에는 영업시간을 입력할 수 없습니다.");
			}
			if (open24Hours && !openingPeriods.isEmpty()) {
				throw invalid("24시간 영업일에는 별도 영업시간을 입력할 수 없습니다.");
			}
			if (!closed && !open24Hours && openingPeriods.isEmpty()) {
				throw invalid("영업일에는 영업시간을 한 개 이상 입력해 주세요.");
			}
			assertNoOverlap(openingPeriods, day + " 영업시간");
			assertNoOverlap(breakPeriods, day + " 휴게시간");
			if (!closed) {
				List<Interval> effectiveOpenings = open24Hours ? List.of(new Interval(0, 1440)) : openingPeriods;
				for (Interval breakPeriod : breakPeriods) {
					if (effectiveOpenings.stream().noneMatch(opening -> opening.contains(breakPeriod))) {
						throw invalid("휴게시간은 영업시간 안에 있어야 합니다.");
					}
				}
			}
		} else if (!closed) {
			String start = requiredText(schedule, "start", day + " 시작 시간");
			String end = requiredText(schedule, "end", day + " 종료 시간");
			interval(start, end, false, day + " 운영시간");
		}

		if (schedule.hasNonNull("last_booking_time")) {
			parseMinute(schedule.path("last_booking_time").asText(), false, day + " 예약 마감 시간");
		}
	}

	private List<Interval> intervals(JsonNode node, String fieldName) {
		if (node.isMissingNode() || node.isNull()) {
			return List.of();
		}
		if (!node.isArray() || node.size() > MAX_DAILY_INTERVALS) {
			throw invalid(fieldName + "은 최대 " + MAX_DAILY_INTERVALS + "개까지 입력할 수 있습니다.");
		}
		List<Interval> result = new ArrayList<>();
		for (JsonNode item : node) {
			if (!item.isObject()) {
				throw invalid(fieldName + " 형식이 올바르지 않습니다.");
			}
			result.add(interval(
				requiredText(item, "start", fieldName + " 시작 시간"),
				requiredText(item, "end", fieldName + " 종료 시간"),
				item.path("ends_next_day").asBoolean(false),
				fieldName
			));
		}
		return result;
	}

	private List<Interval> openingIntervals(JsonNode schedule, String fieldName) {
		if (schedule.isMissingNode() || schedule.path("is_closed").asBoolean(false)) {
			return List.of();
		}
		if (schedule.path("is_24_hours").asBoolean(false)) {
			return List.of(new Interval(0, 1440));
		}
		if (schedule.has("periods")) {
			return intervals(schedule.path("periods"), fieldName);
		}
		return List.of(interval(
			requiredText(schedule, "start", fieldName + " 시작 시간"),
			requiredText(schedule, "end", fieldName + " 종료 시간"),
			false,
			fieldName
		));
	}

	private Interval interval(String start, String end, boolean endsNextDay, String fieldName) {
		int startMinute = parseMinute(start, false, fieldName);
		int endMinute = parseMinute(end, true, fieldName);
		if (endsNextDay) {
			endMinute += 1440;
		}
		if (endMinute <= startMinute || endMinute - startMinute > 1440) {
			throw invalid(fieldName + " 종료 시간은 시작 시간보다 늦어야 합니다.");
		}
		return new Interval(startMinute, endMinute);
	}

	private int parseMinute(String value, boolean allowTwentyFour, String fieldName) {
		if (value == null || !value.matches("^\\d{2}:\\d{2}$")) {
			throw invalid(fieldName + "은 HH:mm 형식이어야 합니다.");
		}
		int hour = Integer.parseInt(value.substring(0, 2));
		int minute = Integer.parseInt(value.substring(3, 5));
		if (minute > 59 || hour > 23 && !(allowTwentyFour && hour == 24 && minute == 0)) {
			throw invalid(fieldName + "이 올바르지 않습니다.");
		}
		return hour * 60 + minute;
	}

	private void assertNoOverlap(List<Interval> intervals, String fieldName) {
		List<Interval> sorted = intervals.stream().sorted((first, second) -> Integer.compare(first.start(), second.start())).toList();
		for (int index = 1; index < sorted.size(); index++) {
			if (sorted.get(index - 1).end() > sorted.get(index).start()) {
				throw invalid(fieldName + "이 서로 겹칩니다.");
			}
		}
	}

	private void validateRegularRules(JsonNode rules) {
		if (rules.isMissingNode() || rules.isNull()) {
			return;
		}
		if (!rules.isArray() || rules.size() > 20) {
			throw invalid("정기 휴무는 최대 20개까지 등록할 수 있습니다.");
		}
		for (JsonNode rule : rules) {
			String frequency = rule.path("frequency").asText();
			if (!REGULAR_FREQUENCIES.contains(frequency)) {
				throw invalid("정기 휴무 주기가 올바르지 않습니다.");
			}
			JsonNode weekdays = rule.path("weekdays");
			if (!weekdays.isArray() || weekdays.isEmpty()) {
				throw invalid("정기 휴무 요일을 선택해 주세요.");
			}
			Set<String> unique = new HashSet<>();
			for (JsonNode weekday : weekdays) {
				if (!weekday.isTextual() || !WEEKDAYS.contains(weekday.asText()) || !unique.add(weekday.asText())) {
					throw invalid("정기 휴무 요일이 올바르지 않습니다.");
				}
			}
		}
	}

	private void validatePublicHolidayPolicy(JsonNode policy) {
		if (policy.isMissingNode() || policy.isNull()) {
			return;
		}
		if (!policy.isObject()) {
			throw invalid("공휴일 휴무 정책 형식이 올바르지 않습니다.");
		}
		JsonNode codes = policy.path("codes");
		if (!codes.isMissingNode() && !codes.isArray()) {
			throw invalid("공휴일 선택값이 올바르지 않습니다.");
		}
		Set<String> unique = new HashSet<>();
		for (JsonNode code : codes) {
			if (!code.isTextual() || !PUBLIC_HOLIDAY_CODES.contains(code.asText()) || !unique.add(code.asText())) {
				throw invalid("공휴일 선택값이 올바르지 않습니다.");
			}
		}
	}

	private void validateSubstituteHolidays(JsonNode holidays) {
		if (holidays.isMissingNode() || holidays.isNull()) {
			return;
		}
		if (!holidays.isArray() || holidays.size() > MAX_HOLIDAY_RULES) {
			throw invalid("대체공휴일 휴무는 최대 " + MAX_HOLIDAY_RULES + "개까지 등록할 수 있습니다.");
		}
		Set<LocalDate> uniqueDates = new HashSet<>();
		for (JsonNode holiday : holidays) {
			LocalDate date = date(holiday.path("date").asText(), "대체공휴일 날짜");
			if (!uniqueDates.add(date)) {
				throw invalid("같은 대체공휴일을 중복 등록할 수 없습니다.");
			}
			if (holiday.isObject()) {
				((ObjectNode) holiday).remove("name");
			}
		}
	}

	private void validateCustomHolidays(JsonNode holidays) {
		if (holidays.isMissingNode() || holidays.isNull()) {
			return;
		}
		if (!holidays.isArray() || holidays.size() > MAX_HOLIDAY_RULES) {
			throw invalid("지정 휴무는 최대 " + MAX_HOLIDAY_RULES + "개까지 등록할 수 있습니다.");
		}
		for (JsonNode holiday : holidays) {
			String recurrence = holiday.path("recurrence").asText();
			if (!CUSTOM_RECURRENCES.contains(recurrence)) {
				throw invalid("지정 휴무 반복 주기가 올바르지 않습니다.");
			}
			LocalDate startDate = date(holiday.path("start_date").asText(), "지정 휴무 시작일");
			LocalDate endDate = date(holiday.path("end_date").asText(), "지정 휴무 종료일");
			if (endDate.isBefore(startDate) || endDate.isAfter(startDate.plusYears(1))) {
				throw invalid("지정 휴무 기간이 올바르지 않습니다.");
			}
			if (holiday.isObject()) {
				((ObjectNode) holiday).remove("name");
			}
		}
	}

	private ObjectNode disabledHolidayPolicy(ObjectNode policy) {
		policy.putArray("regular_rules");
		ObjectNode publicHolidays = policy.putObject("public_holidays");
		publicHolidays.putArray("codes");
		policy.putArray("substitute_holidays");
		policy.putArray("custom_holidays");
		return policy;
	}

	private LocalDate date(String value, String fieldName) {
		try {
			return LocalDate.parse(value);
		} catch (DateTimeException exception) {
			throw invalid(fieldName + "이 올바르지 않습니다.");
		}
	}

	private JsonNode objectNode(Object value, boolean required, String fieldName) {
		if (value == null || value instanceof String stringValue && stringValue.isBlank()) {
			if (required) {
				throw invalid(fieldName + "을 입력해 주세요.");
			}
			return null;
		}
		try {
			JsonNode root = value instanceof String rawValue
				? objectMapper.readTree(rawValue)
				: objectMapper.valueToTree(value);
			if (root == null || !root.isObject()) {
				throw invalid(fieldName + " 형식이 올바르지 않습니다.");
			}
			return root;
		} catch (JsonProcessingException | IllegalArgumentException exception) {
			throw invalid(fieldName + " 형식이 올바르지 않습니다.");
		}
	}

	private String requiredText(JsonNode node, String field, String fieldName) {
		JsonNode value = node.path(field);
		if (!value.isTextual() || value.asText().isBlank()) {
			throw invalid(fieldName + "을 입력해 주세요.");
		}
		return value.asText();
	}

	private String text(JsonNode node, String field, String defaultValue) {
		JsonNode value = node.path(field);
		return value.isTextual() && !value.asText().isBlank() ? value.asText() : defaultValue;
	}

	private String write(JsonNode root, String fieldName) {
		try {
			return objectMapper.writeValueAsString(root);
		} catch (JsonProcessingException exception) {
			throw invalid(fieldName + " 형식이 올바르지 않습니다.");
		}
	}

	private ApiException invalid(String message) {
		return new ApiException(ErrorCode.INVALID_REQUEST, message);
	}

	private record Interval(int start, int end) {
		private boolean contains(Interval other) {
			return start <= other.start && end >= other.end;
		}
	}
}
