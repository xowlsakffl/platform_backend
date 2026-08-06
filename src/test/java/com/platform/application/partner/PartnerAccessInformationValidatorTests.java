package com.platform.application.partner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.common.error.ApiException;
import org.junit.jupiter.api.Test;

class PartnerAccessInformationValidatorTests {

	private final ObjectMapper objectMapper = new ObjectMapper();
	private final PartnerAccessInformationValidator validator = new PartnerAccessInformationValidator(objectMapper);

	@Test
	void normalizesSubwayStationsAndAssignsSortOrder() throws Exception {
		String result = validator.normalizeSubwayStations("""
			[
			  {
			    "external_id": "station-1",
			    "name": "강남역",
			    "line": "2호선",
			    "latitude": "37.4979",
			    "longitude": "127.0276",
			    "exit_number": "11",
			    "distance_meters": 320
			  }
			]
			""");

		JsonNode station = objectMapper.readTree(result).get(0);
		assertThat(station.path("name").asText()).isEqualTo("강남역");
		assertThat(station.path("distance_meters").asInt()).isEqualTo(320);
		assertThat(station.has("exit_number")).isFalse();
		assertThat(station.path("sort_order").asInt()).isZero();
	}

	@Test
	void rejectsMoreThanTwoStations() {
		assertThatThrownBy(() -> validator.normalizeSubwayStations("""
			[
			  {"name":"강남역"},
			  {"name":"역삼역"},
			  {"name":"선릉역"}
			]
			"""))
			.isInstanceOf(ApiException.class)
			.hasMessageContaining("최대 2개");
	}

	@Test
	void rejectsDuplicateExternalIds() {
		assertThatThrownBy(() -> validator.normalizeSubwayStations("""
			[
			  {"external_id":"station-1","name":"강남역"},
			  {"external_id":"station-1","name":"강남역"}
			]
			"""))
			.isInstanceOf(ApiException.class)
			.hasMessageContaining("중복");
	}
}
