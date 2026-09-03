package com.platform.domain.partner;

import static org.assertj.core.api.Assertions.assertThat;

import com.platform.domain.account.AccountStaff;
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

	@Test
	void criticalInformationChangeRestartsAnInProgressReview() {
		Partner partner = Partner.createDraft("테스트 업체");
		partner.requestReview();
		partner.startReview(AccountStaff.create(
			"reviewer",
			"검수자",
			"reviewer",
			"reviewer@platform.local",
			"encoded-password"
		));

		assertThat(partner.restartReviewAfterCriticalInformationChange()).isTrue();
		assertThat(partner.allowStatus()).isEqualTo(PartnerAllowStatus.REVIEW_REQUESTED);
		assertThat(partner.reviewerStaff()).isNull();
		assertThat(partner.reviewStartedAt()).isNull();
	}

	@Test
	void criticalInformationChangeRestartsAnApprovedReview() {
		Partner partner = Partner.createDraft("승인된 업체");
		partner.requestReview();
		partner.startReview(AccountStaff.create(
			"reviewer",
			"검수자",
			"reviewer",
			"reviewer@platform.local",
			"encoded-password"
		));
		partner.completeReview(PartnerAllowStatus.APPROVED);

		assertThat(partner.restartReviewAfterCriticalInformationChange()).isTrue();
		assertThat(partner.allowStatus()).isEqualTo(PartnerAllowStatus.REVIEW_REQUESTED);
		assertThat(partner.reviewerStaff()).isNull();
		assertThat(partner.reviewStartedAt()).isNull();
	}
}
