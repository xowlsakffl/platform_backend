package com.platform.application.partner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.platform.application.partner.result.IssuedPartnerAccountInvitation;
import com.platform.common.security.PartnerAccountInvitationProperties;
import com.platform.domain.partner.Partner;
import com.platform.domain.partner.PartnerAccountInvitation;
import com.platform.domain.partner.PartnerAccountInvitationStatus;
import com.platform.domain.partner.PartnerAllowStatus;
import com.platform.domain.partner.PartnerStatus;
import com.platform.infrastructure.persistence.account.AccountPartnerRepository;
import com.platform.infrastructure.persistence.partner.PartnerAccountInvitationRepository;
import com.platform.infrastructure.persistence.partner.PartnerRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class PartnerAccountInvitationTokenServiceTests {

	private final PartnerRepository partnerRepository = mock(PartnerRepository.class);
	private final AccountPartnerRepository accountPartnerRepository = mock(AccountPartnerRepository.class);
	private final PartnerAccountInvitationRepository invitationRepository = mock(
		PartnerAccountInvitationRepository.class
	);
	private final PartnerAccountInvitationTokenCodec tokenCodec = mock(PartnerAccountInvitationTokenCodec.class);
	private final PartnerAccountInvitationProperties properties = properties();
	private final PartnerAccountInvitationTokenService tokenService = new PartnerAccountInvitationTokenService(
		partnerRepository,
		accountPartnerRepository,
		invitationRepository,
		tokenCodec,
		properties
	);

	@Test
	void reissueKeepsActiveInvitationUntilNewMailIsMarkedSent() {
		LocalDateTime now = LocalDateTime.now();
		Partner partner = partner(7L);
		PartnerAccountInvitation current = invitation(partner, 3L, now.plusHours(1));
		current.markSent(now.minusMinutes(10));
		mockReissue(partner, current);

		IssuedPartnerAccountInvitation issued = tokenService.reissue(1L, partner.id(), current.id());

		assertThat(current.status()).isEqualTo(PartnerAccountInvitationStatus.PENDING);
		assertThat(current.canceledAt()).isNull();
		assertThat(issued.invitationId()).isEqualTo(4L);
		assertThat(issued.partnerId()).isEqualTo(partner.id());
		assertThat(issued.email()).isEqualTo(current.email());
		assertThat(issued.rawToken()).isEqualTo("new-raw-token");
		assertThat(issued.tokenHash()).isEqualTo("new-token-hash");

		PartnerAccountInvitation reissued = invitation(partner, 4L, now.plusHours(1));
		ReflectionTestUtils.setField(reissued, "tokenHash", "new-token-hash");
		when(invitationRepository.findForUpdateByIdAndPartner_Id(issued.invitationId(), partner.id()))
			.thenReturn(Optional.of(reissued));
		when(invitationRepository.findByPartner_IdAndStatus(partner.id(), PartnerAccountInvitationStatus.PENDING))
			.thenReturn(List.of(current, reissued));

		tokenService.markSentAndCancelPrevious(issued);

		assertThat(current.status()).isEqualTo(PartnerAccountInvitationStatus.CANCELED);
		assertThat(current.canceledAt()).isNotNull();
		assertThat(reissued.status()).isEqualTo(PartnerAccountInvitationStatus.PENDING);
		assertThat(reissued.sentAt()).isNotNull();
	}

	@Test
	void reissuePreservesExpiredInvitationAsPendingHistory() {
		LocalDateTime now = LocalDateTime.now();
		Partner partner = partner(7L);
		PartnerAccountInvitation expired = invitation(partner, 3L, now.minusHours(1));
		mockReissue(partner, expired);

		IssuedPartnerAccountInvitation issued = tokenService.reissue(1L, partner.id(), expired.id());

		assertThat(expired.status()).isEqualTo(PartnerAccountInvitationStatus.PENDING);
		assertThat(expired.isExpired(LocalDateTime.now())).isTrue();
		assertThat(expired.canceledAt()).isNull();
		assertThat(issued.invitationId()).isEqualTo(4L);
	}

	private void mockReissue(Partner partner, PartnerAccountInvitation current) {
		when(partnerRepository.findForUpdateByIdAndDeletedAtIsNull(partner.id())).thenReturn(Optional.of(partner));
		when(accountPartnerRepository.findForUpdateByPartner_IdAndDeletedAtIsNull(partner.id()))
			.thenReturn(Optional.empty());
		when(accountPartnerRepository.existsByEmail(current.email())).thenReturn(false);
		when(invitationRepository.findForUpdateByIdAndPartner_Id(current.id(), partner.id()))
			.thenReturn(Optional.of(current));
		when(invitationRepository.findByEmailAndStatus(current.email(), PartnerAccountInvitationStatus.PENDING))
			.thenReturn(List.of(current));
		when(invitationRepository.findByPartner_IdAndStatus(partner.id(), PartnerAccountInvitationStatus.PENDING))
			.thenReturn(List.of(current));
		when(tokenCodec.newRawToken()).thenReturn("new-raw-token");
		when(tokenCodec.hash("new-raw-token")).thenReturn("new-token-hash");
		when(invitationRepository.saveAndFlush(any(PartnerAccountInvitation.class))).thenAnswer(invocation -> {
			PartnerAccountInvitation saved = invocation.getArgument(0);
			if (saved.id() != null) {
				return saved;
			}
			ReflectionTestUtils.setField(saved, "id", 4L);
			assertThat(saved).isNotSameAs(current);
			assertThat(saved.status()).isEqualTo(PartnerAccountInvitationStatus.PENDING);
			assertThat(saved.email()).isEqualTo(current.email());
			assertThat(saved.tokenHash()).isEqualTo("new-token-hash");
			assertThat(saved.sentAt()).isNull();
			assertThat(saved.expiresAt()).isAfter(LocalDateTime.now());
			return saved;
		});
	}

	private Partner partner(Long id) {
		Partner partner = new Partner(
			"Test partner",
			null,
			null,
			null,
			null,
			null,
			null,
			null,
			PartnerAllowStatus.APPROVED,
			PartnerStatus.ACTIVE
		);
		ReflectionTestUtils.setField(partner, "id", id);
		return partner;
	}

	private PartnerAccountInvitation invitation(Partner partner, Long id, LocalDateTime expiresAt) {
		PartnerAccountInvitation invitation = PartnerAccountInvitation.create(
			partner,
			"owner@example.com",
			"old-token-hash",
			expiresAt,
			1L
		);
		ReflectionTestUtils.setField(invitation, "id", id);
		return invitation;
	}

	private PartnerAccountInvitationProperties properties() {
		PartnerAccountInvitationProperties properties = new PartnerAccountInvitationProperties();
		properties.setTokenTtlSeconds(3_600);
		return properties;
	}
}
