package com.platform.domain.specialist;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SpecialistAllowStatusPolicyTests {

	@Test
	void reviewMustStartBeforeItCanBeApprovedOrRejected() {
		assertThat(SpecialistAllowStatus.REVIEW_REQUESTED.canTransitionTo(SpecialistAllowStatus.IN_REVIEW)).isTrue();
		assertThat(SpecialistAllowStatus.IN_REVIEW.canTransitionTo(SpecialistAllowStatus.APPROVED)).isTrue();
		assertThat(SpecialistAllowStatus.IN_REVIEW.canTransitionTo(SpecialistAllowStatus.REJECTED)).isTrue();
		assertThat(SpecialistAllowStatus.REVIEW_REQUESTED.canTransitionTo(SpecialistAllowStatus.APPROVED)).isFalse();
		assertThat(SpecialistAllowStatus.APPROVED.canTransitionTo(SpecialistAllowStatus.REJECTED)).isFalse();
	}
}
