package com.platform.application.partner;

import com.platform.application.partner.result.IssuedPartnerAccountInvitation;
import com.platform.common.error.ApiException;
import com.platform.common.error.ErrorCode;
import com.platform.common.security.PartnerAccountInvitationProperties;
import com.platform.domain.partner.Partner;
import com.platform.domain.partner.PartnerAccountInvitation;
import com.platform.domain.partner.PartnerAccountInvitationStatus;
import com.platform.domain.partner.PartnerStatus;
import com.platform.infrastructure.persistence.account.AccountPartnerRepository;
import com.platform.infrastructure.persistence.partner.PartnerAccountInvitationRepository;
import com.platform.infrastructure.persistence.partner.PartnerRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PartnerAccountInvitationTokenService {

	private final PartnerRepository partnerRepository;
	private final AccountPartnerRepository accountPartnerRepository;
	private final PartnerAccountInvitationRepository invitationRepository;
	private final PartnerAccountInvitationTokenCodec tokenCodec;
	private final PartnerAccountInvitationProperties properties;

	public PartnerAccountInvitationTokenService(
		PartnerRepository partnerRepository,
		AccountPartnerRepository accountPartnerRepository,
		PartnerAccountInvitationRepository invitationRepository,
		PartnerAccountInvitationTokenCodec tokenCodec,
		PartnerAccountInvitationProperties properties
	) {
		this.partnerRepository = partnerRepository;
		this.accountPartnerRepository = accountPartnerRepository;
		this.invitationRepository = invitationRepository;
		this.tokenCodec = tokenCodec;
		this.properties = properties;
	}

	@Transactional
	public IssuedPartnerAccountInvitation issue(
		Long staffId,
		Long partnerId,
		String email
	) {
		Partner partner = lockInvitablePartner(partnerId);
		assertAccountNotConnected(partnerId);
		assertEmailAvailable(email);
		LocalDateTime now = LocalDateTime.now();
		assertNoOtherActiveInvitation(email, partnerId, null, now);

		String rawToken = tokenCodec.newRawToken();
		String tokenHash = tokenCodec.hash(rawToken);
		PartnerAccountInvitation invitation = invitationRepository.saveAndFlush(
			PartnerAccountInvitation.create(
				partner,
				email,
				tokenHash,
				now.plusSeconds(properties.tokenTtlSeconds()),
				staffId
			)
		);
		return issued(invitation, rawToken, tokenHash);
	}

	@Transactional
	public IssuedPartnerAccountInvitation reissue(Long staffId, Long partnerId, Long invitationId) {
		Partner partner = lockInvitablePartner(partnerId);
		assertAccountNotConnected(partnerId);
		PartnerAccountInvitation invitation = invitationRepository
			.findForUpdateByIdAndPartner_Id(invitationId, partnerId)
			.orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Invitation not found."));
		if (invitation.status() == PartnerAccountInvitationStatus.ACCEPTED
			|| invitation.status() == PartnerAccountInvitationStatus.CANCELED) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "This invitation cannot be resent.");
		}
		assertEmailAvailable(invitation.email());
		LocalDateTime now = LocalDateTime.now();
		assertNoOtherActiveInvitation(invitation.email(), partnerId, invitation.id(), now);

		String rawToken = tokenCodec.newRawToken();
		String tokenHash = tokenCodec.hash(rawToken);
		PartnerAccountInvitation reissuedInvitation = invitationRepository.saveAndFlush(
			PartnerAccountInvitation.create(
				partner,
				invitation.email(),
				tokenHash,
				now.plusSeconds(properties.tokenTtlSeconds()),
				staffId
			)
		);
		return issued(reissuedInvitation, rawToken, tokenHash);
	}

	@Transactional
	public PartnerAccountInvitation markSentAndCancelPrevious(IssuedPartnerAccountInvitation issued) {
		lockInvitablePartner(issued.partnerId());
		assertAccountNotConnected(issued.partnerId());
		PartnerAccountInvitation invitation = lockIssued(issued);
		LocalDateTime now = LocalDateTime.now();
		invitation.markSent(now);
		cancelPendingInvitations(issued.partnerId(), invitation.id(), now);
		return invitationRepository.saveAndFlush(invitation);
	}

	@Transactional
	public void cancelIssued(IssuedPartnerAccountInvitation issued) {
		invitationRepository.findForUpdateByIdAndPartner_Id(issued.invitationId(), issued.partnerId())
			.filter(invitation -> constantTimeEquals(invitation.tokenHash(), issued.tokenHash()))
			.filter(invitation -> invitation.status() == PartnerAccountInvitationStatus.PENDING)
			.ifPresent(invitation -> invitation.cancel(LocalDateTime.now()));
	}

	private Partner lockInvitablePartner(Long partnerId) {
		Partner partner = partnerRepository.findForUpdateByIdAndDeletedAtIsNull(partnerId)
			.orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Partner not found."));
		if (partner.status() == PartnerStatus.WITHDRAWN) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "A withdrawn partner cannot receive invitations.");
		}
		return partner;
	}

	private void assertAccountNotConnected(Long partnerId) {
		if (accountPartnerRepository.findForUpdateByPartner_IdAndDeletedAtIsNull(partnerId).isPresent()) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "A partner account is already connected.");
		}
	}

	private void assertEmailAvailable(String email) {
		if (accountPartnerRepository.existsByEmail(email)) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "Email is already in use.");
		}
	}

	private void assertNoOtherActiveInvitation(
		String email,
		Long partnerId,
		Long currentInvitationId,
		LocalDateTime now
	) {
		boolean conflict = invitationRepository
			.findByEmailAndStatus(email, PartnerAccountInvitationStatus.PENDING)
			.stream()
			.filter(invitation -> currentInvitationId == null || !currentInvitationId.equals(invitation.id()))
			.anyMatch(invitation -> !invitation.isExpired(now) && !partnerId.equals(invitation.partnerId()));
		if (conflict) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "Email has another active partner invitation.");
		}
	}

	private void cancelPendingInvitations(Long partnerId, Long currentInvitationId, LocalDateTime now) {
		List<PartnerAccountInvitation> invitations = invitationRepository.findByPartner_IdAndStatus(
			partnerId,
			PartnerAccountInvitationStatus.PENDING
		);
		invitations.stream()
			.filter(invitation -> !invitation.id().equals(currentInvitationId))
			.filter(invitation -> !invitation.isExpired(now))
			.forEach(invitation -> invitation.cancel(now));
	}

	private PartnerAccountInvitation lockIssued(IssuedPartnerAccountInvitation issued) {
		return invitationRepository.findForUpdateByIdAndPartner_Id(issued.invitationId(), issued.partnerId())
			.filter(invitation -> constantTimeEquals(invitation.tokenHash(), issued.tokenHash()))
			.filter(invitation -> invitation.status() == PartnerAccountInvitationStatus.PENDING)
			.orElseThrow(() -> new ApiException(ErrorCode.TOKEN_ERROR, "Invitation token is no longer valid."));
	}

	private IssuedPartnerAccountInvitation issued(
		PartnerAccountInvitation invitation,
		String rawToken,
		String tokenHash
	) {
		return new IssuedPartnerAccountInvitation(
			invitation.id(),
			invitation.partnerId(),
			invitation.partner().name(),
			invitation.email(),
			rawToken,
			tokenHash
		);
	}

	private boolean constantTimeEquals(String expected, String actual) {
		return expected != null && actual != null && MessageDigest.isEqual(
			expected.getBytes(StandardCharsets.UTF_8),
			actual.getBytes(StandardCharsets.UTF_8)
		);
	}
}
