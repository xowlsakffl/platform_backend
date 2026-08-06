package com.platform.application.partner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.platform.common.error.ApiException;
import org.junit.jupiter.api.Test;

class PartnerBusinessNumberPolicyTests {

	private final PartnerBusinessNumberPolicy policy = new PartnerBusinessNumberPolicy();

	@Test
	void normalizesFormattedAndUnformattedNumbers() {
		assertThat(policy.normalize("123-45-67890")).isEqualTo("1234567890");
		assertThat(policy.normalize("1234567890")).isEqualTo("1234567890");
	}

	@Test
	void rejectsMissingOrInvalidLengthNumbers() {
		assertThatThrownBy(() -> policy.normalize(null)).isInstanceOf(ApiException.class);
		assertThatThrownBy(() -> policy.normalize("123-45-6789")).isInstanceOf(ApiException.class);
		assertThatThrownBy(() -> policy.normalize("사업자번호 없음")).isInstanceOf(ApiException.class);
	}
}
