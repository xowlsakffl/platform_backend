package com.platform.domain.partner;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PartnerStatusPolicyTests {

	@Test
	void reviewMustStartBeforeItCanBeApprovedOrRejected() {
		assertThat(PartnerAllowStatus.REVIEW_REQUESTED.canTransitionTo(PartnerAllowStatus.IN_REVIEW)).isTrue();
		assertThat(PartnerAllowStatus.IN_REVIEW.canTransitionTo(PartnerAllowStatus.APPROVED)).isTrue();
		assertThat(PartnerAllowStatus.IN_REVIEW.canTransitionTo(PartnerAllowStatus.REJECTED)).isTrue();
		assertThat(PartnerAllowStatus.REVIEW_REQUESTED.canTransitionTo(PartnerAllowStatus.APPROVED)).isFalse();
		assertThat(PartnerAllowStatus.APPROVED.canTransitionTo(PartnerAllowStatus.REJECTED)).isFalse();
	}

	@Test
	void withdrawnIsNotAStaffSelectableOperationStatus() {
		assertThat(PartnerStatus.ACTIVE.staffSelectable()).isTrue();
		assertThat(PartnerStatus.SUSPENDED.staffSelectable()).isTrue();
		assertThat(PartnerStatus.WITHDRAWN.staffSelectable()).isFalse();
	}
}
