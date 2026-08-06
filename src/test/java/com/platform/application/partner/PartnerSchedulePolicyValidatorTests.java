package com.platform.application.partner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.common.error.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PartnerSchedulePolicyValidatorTests {

	private PartnerSchedulePolicyValidator validator;

	@BeforeEach
	void setUp() {
		validator = new PartnerSchedulePolicyValidator(new ObjectMapper());
	}

	@Test
	void normalizesDetailedOperationHours() {
		String schedule = """
			{
			  "timezone":"Asia/Seoul",
			  "reservation_only":true,
			  "mon":{"is_closed":false,"is_24_hours":false,"periods":[{"start":"10:00","end":"19:00","ends_next_day":false}],"breaks":[{"start":"13:00","end":"14:00","ends_next_day":false}]},
			  "tue":{"is_closed":false,"is_24_hours":false,"periods":[{"start":"10:00","end":"19:00"}],"breaks":[]},
			  "wed":{"is_closed":false,"is_24_hours":false,"periods":[{"start":"10:00","end":"19:00"}],"breaks":[]},
			  "thu":{"is_closed":false,"is_24_hours":false,"periods":[{"start":"10:00","end":"19:00"}],"breaks":[]},
			  "fri":{"is_closed":false,"is_24_hours":false,"periods":[{"start":"10:00","end":"02:00","ends_next_day":true}],"breaks":[]},
			  "sat":{"is_closed":false,"is_24_hours":true,"periods":[],"breaks":[]},
			  "sun":{"is_closed":true,"is_24_hours":false,"periods":[],"breaks":[]}
			}
			""";

		assertThat(validator.normalizeOperationHours(schedule, true))
			.contains("\"timezone\":\"Asia/Seoul\"")
			.contains("\"ends_next_day\":true")
			.doesNotContain("reservation_only");
	}

	@Test
	void doesNotAddReservationOnly() {
		String schedule = """
			{
			  "mon":{"is_closed":true},"tue":{"is_closed":true},"wed":{"is_closed":true},
			  "thu":{"is_closed":true},"fri":{"is_closed":true},"sat":{"is_closed":true},
			  "sun":{"is_closed":true}
			}
			""";

		assertThat(validator.normalizeOperationHours(schedule, true))
			.doesNotContain("reservation_only");
	}

	@Test
	void rejectsBreakOutsideOpeningPeriod() {
		String schedule = """
			{
			  "mon":{"is_closed":false,"periods":[{"start":"10:00","end":"19:00"}],"breaks":[{"start":"19:00","end":"20:00"}]},
			  "tue":{"is_closed":true},"wed":{"is_closed":true},"thu":{"is_closed":true},
			  "fri":{"is_closed":true},"sat":{"is_closed":true},"sun":{"is_closed":true}
			}
			""";

		assertThatThrownBy(() -> validator.normalizeOperationHours(schedule, true))
			.isInstanceOf(ApiException.class)
			.hasMessageContaining("휴게시간은 영업시간 안에");
	}

	@Test
	void rejectsHiddenOpeningValuesOnClosedDay() {
		String schedule = """
			{
			  "mon":{"is_closed":true,"is_24_hours":false,"periods":[{"start":"10:00","end":"19:00"}],"breaks":[]},
			  "tue":{"is_closed":true},"wed":{"is_closed":true},"thu":{"is_closed":true},
			  "fri":{"is_closed":true},"sat":{"is_closed":true},"sun":{"is_closed":true}
			}
			""";

		assertThatThrownBy(() -> validator.normalizeOperationHours(schedule, true))
			.isInstanceOf(ApiException.class)
			.hasMessageContaining("휴무일에는 영업시간을 입력할 수 없습니다");
	}

	@Test
	void normalizesHolidayPolicy() {
		String policy = """
			{
			  "enabled":true,
			  "regular_rules":[{"frequency":"MONTHLY_SECOND","weekdays":["SUN"]}],
			  "public_holidays":{"codes":["LUNAR_NEW_YEAR_EVE","LUNAR_NEW_YEAR","LABOR_DAY","CHUSEOK"]},
			  "substitute_holidays":[{"date":"2026-08-17","name":"삭제 대상"}],
			  "custom_holidays":[{"recurrence":"ONCE","start_date":"2026-08-20","end_date":"2026-08-21","name":"내부 공사"}]
			}
			""";

		assertThat(validator.normalizeHolidayPolicy(policy, true))
			.contains("MONTHLY_SECOND")
			.contains("2026-08-20")
			.doesNotContain("삭제 대상", "내부 공사", "\"name\"");
	}

	@Test
	void rejectsFifthMonthlyHolidayFrequency() {
		String policy = """
			{
			  "enabled":true,
			  "regular_rules":[{"frequency":"MONTHLY_FIFTH","weekdays":["SUN"]}]
			}
			""";

		assertThatThrownBy(() -> validator.normalizeHolidayPolicy(policy, true))
			.isInstanceOf(ApiException.class)
			.hasMessageContaining("주기가 올바르지 않습니다");
	}

	@Test
	void clearsStaleHolidayRulesWhenHolidayPolicyIsDisabled() {
		String policy = """
			{
			  "enabled":false,
			  "regular_rules":[{"frequency":"WEEKLY","weekdays":[]}],
			  "substitute_holidays":[{"date":"invalid"}]
			}
			""";

		assertThat(validator.normalizeHolidayPolicy(policy, true))
			.contains("\"regular_rules\":[]")
			.contains("\"substitute_holidays\":[]");
	}
}
