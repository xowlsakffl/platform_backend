package com.platform.domain.partner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class PartnerAccountInvitationTests {

	@Test
	void selfOnboardingPartnerKeepsItsRegistrationSource() {
		Partner partner = Partner.createDraft("Self partner");

		assertThat(partner.registrationSource()).isEqualTo(PartnerRegistrationSource.SELF_ONBOARDING);
		assertThat(partner.createdByStaffId()).isNull();
	}

	@Test
	void staffCreatedPartnerRecordsItsCreator() {
		Partner partner = Partner.createDraft("Staff partner");

		partner.markStaffCreated(17L);

		assertThat(partner.registrationSource()).isEqualTo(PartnerRegistrationSource.STAFF_CREATED);
		assertThat(partner.createdByStaffId()).isEqualTo(17L);
	}

	@Test
	void pendingInvitationBecomesEffectivelyExpiredWithoutChangingStoredStatus() {
		LocalDateTime now = LocalDateTime.now();
		PartnerAccountInvitation invitation = invitation(now.minusSeconds(1));

		assertThat(invitation.status()).isEqualTo(PartnerAccountInvitationStatus.PENDING);
		assertThat(invitation.effectiveStatus(now)).isEqualTo(PartnerAccountInvitationStatus.EXPIRED);
	}

	@Test
	void acceptedInvitationCannotBeCanceledOrReissued() {
		LocalDateTime now = LocalDateTime.now();
		PartnerAccountInvitation invitation = invitation(now.plusHours(1));
		invitation.accept(now);

		assertThat(invitation.status()).isEqualTo(PartnerAccountInvitationStatus.ACCEPTED);
		assertThatThrownBy(() -> invitation.cancel(now.plusMinutes(1)))
			.isInstanceOf(IllegalStateException.class);
		assertThatThrownBy(() -> invitation.reissue("new-hash", now.plusHours(2), 1L))
			.isInstanceOf(IllegalStateException.class);
	}

	private PartnerAccountInvitation invitation(LocalDateTime expiresAt) {
		return PartnerAccountInvitation.create(
			Partner.createDraft("Partner"),
			"owner@example.com",
			"Owner",
			"token-hash",
			expiresAt,
			1L
		);
	}
}
