package com.platform.application.partner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.platform.application.auth.PermissionService;
import com.platform.application.cache.StaffSummaryCacheInvalidator;
import com.platform.application.partner.command.CreatePartnerAccountInvitationCommand;
import com.platform.common.security.AuthenticatedActor;
import com.platform.common.security.PartnerAccountInvitationProperties;
import com.platform.domain.account.AccountActorType;
import com.platform.domain.partner.Partner;
import com.platform.domain.partner.PartnerAccountInvitation;
import com.platform.domain.partner.PartnerAccountInvitationDeliveryStatus;
import com.platform.domain.partner.PartnerAccountInvitationStatus;
import com.platform.infrastructure.persistence.account.AccountPartnerRepository;
import com.platform.infrastructure.persistence.account.AccountStaffRepository;
import com.platform.infrastructure.persistence.operationhistory.OperationHistoryRepository;
import com.platform.infrastructure.persistence.partner.PartnerAccountInvitationRepository;
import com.platform.infrastructure.persistence.partner.PartnerRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

class PartnerAccountInvitationServiceTests {

	@Test
	void mailFailureCancelsOnlyNewInvitationAndKeepsExistingLinkValid() {
		Partner partner = Partner.createDraft("Partner");
		ReflectionTestUtils.setField(partner, "id", 7L);
		PartnerAccountInvitation existing = invitation(partner, 3L, "old-token", LocalDateTime.now().plusHours(1));
		existing.markSent(LocalDateTime.now().minusMinutes(10));

		PartnerRepository partnerRepository = mock(PartnerRepository.class);
		AccountPartnerRepository accountPartnerRepository = mock(AccountPartnerRepository.class);
		PartnerAccountInvitationRepository invitationRepository = mock(PartnerAccountInvitationRepository.class);
		PartnerAccountInvitationTokenCodec tokenCodec = mock(PartnerAccountInvitationTokenCodec.class);
		PartnerAccountInvitationProperties properties = new PartnerAccountInvitationProperties();
		properties.setTokenTtlSeconds(3_600);
		AtomicReference<PartnerAccountInvitation> created = new AtomicReference<>();

		when(partnerRepository.findForUpdateByIdAndDeletedAtIsNull(partner.id())).thenReturn(Optional.of(partner));
		when(accountPartnerRepository.findForUpdateByPartner_IdAndDeletedAtIsNull(partner.id()))
			.thenReturn(Optional.empty());
		when(accountPartnerRepository.existsByEmail("owner@example.com")).thenReturn(false);
		when(invitationRepository.findByEmailAndStatus("owner@example.com", PartnerAccountInvitationStatus.PENDING))
			.thenReturn(List.of(existing));
		when(tokenCodec.newRawToken()).thenReturn("new-raw-token");
		when(tokenCodec.hash("new-raw-token")).thenReturn("new-token-hash");
		when(invitationRepository.saveAndFlush(any(PartnerAccountInvitation.class))).thenAnswer(invocation -> {
			PartnerAccountInvitation invitation = invocation.getArgument(0);
			ReflectionTestUtils.setField(invitation, "id", 4L);
			created.set(invitation);
			return invitation;
		});
		when(invitationRepository.findForUpdateByIdAndPartner_Id(4L, partner.id()))
			.thenAnswer(ignored -> Optional.ofNullable(created.get()));

		PartnerAccountInvitationTokenService tokenService = new PartnerAccountInvitationTokenService(
			partnerRepository,
			accountPartnerRepository,
			invitationRepository,
			tokenCodec,
			properties
		);
		PartnerAccountInvitationMailSender mailSender = mock(PartnerAccountInvitationMailSender.class);
		org.mockito.Mockito.doThrow(new RuntimeException("mail unavailable"))
			.when(mailSender)
			.send(any(), any(), any(), anyLong());
		PartnerAccountInvitationService service = new PartnerAccountInvitationService(
			mock(PermissionService.class),
			partnerRepository,
			accountPartnerRepository,
			mock(AccountStaffRepository.class),
			invitationRepository,
			tokenService,
			tokenCodec,
			mailSender,
			properties,
			mock(PasswordEncoder.class),
			mock(OperationHistoryRepository.class),
			mock(StaffSummaryCacheInvalidator.class)
		);
		AuthenticatedActor actor = new AuthenticatedActor(
			AccountActorType.STAFF,
			1L,
			null,
			"session",
			"staff@example.com",
			"platform_staff",
			"Staff",
			"staff",
			Set.of()
		);

		assertThatThrownBy(() -> service.invite(
			actor,
			partner.id(),
			new CreatePartnerAccountInvitationCommand("owner@example.com")
		)).hasMessage("mail unavailable");

		assertThat(existing.status()).isEqualTo(PartnerAccountInvitationStatus.PENDING);
		assertThat(existing.deliveryStatus()).isEqualTo(PartnerAccountInvitationDeliveryStatus.SENT);
		assertThat(existing.canceledAt()).isNull();
		assertThat(created.get().status()).isEqualTo(PartnerAccountInvitationStatus.CANCELED);
		assertThat(created.get().deliveryStatus()).isEqualTo(PartnerAccountInvitationDeliveryStatus.FAILED);
	}

	private PartnerAccountInvitation invitation(
		Partner partner,
		Long id,
		String tokenHash,
		LocalDateTime expiresAt
	) {
		PartnerAccountInvitation invitation = PartnerAccountInvitation.create(
			partner,
			"owner@example.com",
			tokenHash,
			expiresAt,
			1L
		);
		ReflectionTestUtils.setField(invitation, "id", id);
		return invitation;
	}
}
