package com.platform.domain.partner;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PartnerRegionSortKeyTests {

	@Test
	void usesCityAndDistrictForMetropolitanAddress() {
		Partner partner = partner("서울 강남구 테헤란로 123", null);

		assertThat(partner.regionSortKey()).isEqualTo("서울 강남구");
	}

	@Test
	void includesDistrictForProvinceCityDistrictAddress() {
		Partner partner = partner("경기 수원시 팔달구 효원로 1", null);

		assertThat(partner.regionSortKey()).isEqualTo("경기 수원시 팔달구");
	}

	@Test
	void fallsBackToJibunAddress() {
		Partner partner = partner(null, "부산 해운대구 우동 1");

		assertThat(partner.regionSortKey()).isEqualTo("부산 해운대구");
	}

	private Partner partner(String roadAddress, String jibunAddress) {
		return new Partner(
			"Partner",
			null,
			roadAddress,
			jibunAddress,
			null,
			null,
			null,
			null,
			null,
			PartnerAllowStatus.APPROVED,
			PartnerStatus.ACTIVE
		);
	}
}
