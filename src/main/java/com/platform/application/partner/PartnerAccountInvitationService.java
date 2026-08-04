package com.platform.application.partner;

import com.platform.application.auth.PermissionService;
import com.platform.application.cache.StaffSummaryCache;
import com.platform.application.cache.StaffSummaryCacheInvalidator;
import com.platform.application.partner.command.AcceptPartnerAccountInvitationCommand;
import com.platform.application.partner.command.CreatePartnerAccountInvitationCommand;
import com.platform.application.partner.result.IssuedPartnerAccountInvitation;
import com.platform.application.partner.result.PartnerAccountInvitationAcceptedResult;
import com.platform.application.partner.result.PartnerAccountInvitationResult;
import com.platform.application.partner.result.PartnerAccountInvitationVerifyResult;
import com.platform.common.error.ApiException;
import com.platform.common.error.ErrorCode;
import com.platform.common.security.AccessPermissions;
import com.platform.common.security.AuthenticatedActor;
import com.platform.common.security.PartnerAccountInvitationProperties;
import com.platform.domain.account.AccountActorType;
import com.platform.domain.account.AccountPartner;
import com.platform.domain.account.AccountPartnerStatus;
import com.platform.domain.operationhistory.OperationHistory;
import com.platform.domain.partner.Partner;
import com.platform.domain.partner.PartnerAccountInvitation;
import com.platform.domain.partner.PartnerAccountInvitationStatus;
import com.platform.domain.partner.PartnerStatus;
import com.platform.infrastructure.persistence.account.AccountPartnerRepository;
import com.platform.infrastructure.persistence.operationhistory.OperationHistoryRepository;
import com.platform.infrastructure.persistence.partner.PartnerAccountInvitationRepository;
import com.platform.infrastructure.persistence.partner.PartnerRepository;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class PartnerAccountInvitationService {

	private static final String ACTION_INVITATION_SENT = "ACCOUNT_INVITATION_SENT";
	private static final String ACTION_INVITATION_RESENT = "ACCOUNT_INVITATION_RESENT";
	private static final String ACTION_INVITATION_CANCELED = "ACCOUNT_INVITATION_CANCELED";
	private static final String ACTION_INVITATION_ACCEPTED = "ACCOUNT_INVITATION_ACCEPTED";

	private final PermissionService permissionService;
	private final PartnerRepository partnerRepository;
	private final AccountPartnerRepository accountPartnerRepository;
	private final PartnerAccountInvitationRepository invitationRepository;
	private final PartnerAccountInvitationTokenService tokenService;
	private final PartnerAccountInvitationTokenCodec tokenCodec;
	private final PartnerAccountInvitationMailSender mailSender;
	private final PartnerAccountInvitationProperties properties;
	private final PasswordEncoder passwordEncoder;
	private final OperationHistoryRepository historyRepository;
	private final StaffSummaryCacheInvalidator summaryCacheInvalidator;

	public PartnerAccountInvitationService(
		PermissionService permissionService,
		PartnerRepository partnerRepository,
		AccountPartnerRepository accountPartnerRepository,
		PartnerAccountInvitationRepository invitationRepository,
		PartnerAccountInvitationTokenService tokenService,
		PartnerAccountInvitationTokenCodec tokenCodec,
		PartnerAccountInvitationMailSender mailSender,
		PartnerAccountInvitationProperties properties,
		PasswordEncoder passwordEncoder,
		OperationHistoryRepository historyRepository,
		StaffSummaryCacheInvalidator summaryCacheInvalidator
	) {
		this.permissionService = permissionService;
		this.partnerRepository = partnerRepository;
		this.accountPartnerRepository = accountPartnerRepository;
		this.invitationRepository = invitationRepository;
		this.tokenService = tokenService;
		this.tokenCodec = tokenCodec;
		this.mailSender = mailSender;
		this.properties = properties;
		this.passwordEncoder = passwordEncoder;
		this.historyRepository = historyRepository;
		this.summaryCacheInvalidator = summaryCacheInvalidator;
	}

	@Transactional(readOnly = true)
	public List<PartnerAccountInvitationResult> list(AuthenticatedActor actor, Long partnerId) {
		permissionService.requireStaffPermission(actor, AccessPermissions.PARTNER_SHOW);
		findPartner(partnerId);
		LocalDateTime now = LocalDateTime.now();
		return invitationRepository.findByPartner_IdOrderByCreatedAtDescIdDesc(partnerId)
			.stream()
			.map(invitation -> result(invitation, now))
			.toList();
	}

	public PartnerAccountInvitationResult invite(
		AuthenticatedActor actor,
		Long partnerId,
		CreatePartnerAccountInvitationCommand command
	) {
		permissionService.requireStaffPermission(actor, AccessPermissions.PARTNER_UPDATE);
		String email = normalizeEmail(command.email());
		String recipientName = trimToNull(command.recipientName());
		IssuedPartnerAccountInvitation issued = tokenService.issue(
			actor.accountId(),
			partnerId,
			email,
			recipientName
		);
		return send(actor, issued, ACTION_INVITATION_SENT);
	}

	public PartnerAccountInvitationResult resend(
		AuthenticatedActor actor,
		Long partnerId,
		Long invitationId
	) {
		permissionService.requireStaffPermission(actor, AccessPermissions.PARTNER_UPDATE);
		IssuedPartnerAccountInvitation issued = tokenService.reissue(actor.accountId(), partnerId, invitationId);
		return send(actor, issued, ACTION_INVITATION_RESENT);
	}

	public PartnerAccountInvitationResult cancel(
		AuthenticatedActor actor,
		Long partnerId,
		Long invitationId
	) {
		permissionService.requireStaffPermission(actor, AccessPermissions.PARTNER_UPDATE);
		PartnerAccountInvitation invitation = tokenService.cancel(partnerId, invitationId);
		recordHistory(actor.actorType().name(), actor.accountId(), partnerId, ACTION_INVITATION_CANCELED, invitation.email());
		return result(invitation, partnerId, LocalDateTime.now());
	}

	@Transactional(readOnly = true)
	public PartnerAccountInvitationVerifyResult verify(String rawToken) {
		PartnerAccountInvitation invitation = validInvitation(rawToken, false);
		return new PartnerAccountInvitationVerifyResult(
			true,
			invitation.partner().name(),
			maskEmail(invitation.email()),
			invitation.expiresAt()
		);
	}

	@Transactional
	public PartnerAccountInvitationAcceptedResult accept(AcceptPartnerAccountInvitationCommand command) {
		String rawToken = requireText(command.token(), "Invitation token is required.");
		PartnerAccountInvitation invitation = validInvitation(rawToken, true);
		Partner partner = invitation.partner();
		String name = requireText(command.name(), "Name is required.");
		String nickname = requireText(command.nickname(), "Nickname is required.");
		String phone = trimToNull(command.phone());
		String password = requireText(command.password(), "Password is required.");
		validatePassword(password);

		if (accountPartnerRepository.findForUpdateByPartner_IdAndDeletedAtIsNull(partner.id()).isPresent()) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "A partner account is already connected.");
		}
		if (accountPartnerRepository.existsByEmail(invitation.email())) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "Email is already in use.");
		}
		if (accountPartnerRepository.existsByNickname(nickname)) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "Nickname is already in use.");
		}

		AccountPartner account = accountPartnerRepository.saveAndFlush(AccountPartner.create(
			partner,
			name,
			nickname,
			invitation.email(),
			phone,
			passwordEncoder.encode(password),
			AccountPartnerStatus.ACTIVE
		));
		invitation.accept(LocalDateTime.now());
		invitationRepository.saveAndFlush(invitation);
		recordHistory(
			AccountActorType.PARTNER.name(),
			account.id(),
			partner.id(),
			ACTION_INVITATION_ACCEPTED,
			invitation.email()
		);
		summaryCacheInvalidator.forgetAfterCommit(StaffSummaryCache.PARTNER);
		return new PartnerAccountInvitationAcceptedResult(
			partner.id(),
			partner.name(),
			invitation.email(),
			partner.allowStatus().name(),
			"Partner account has been created."
		);
	}

	private PartnerAccountInvitationResult send(
		AuthenticatedActor actor,
		IssuedPartnerAccountInvitation issued,
		String action
	) {
		try {
			mailSender.send(
				issued.email(),
				issued.recipientName(),
				issued.partnerName(),
				setupUrl(issued.rawToken()),
				Math.max(1, properties.tokenTtlSeconds() / 3_600)
			);
			PartnerAccountInvitation invitation = tokenService.markSent(issued);
			recordHistory(actor.actorType().name(), actor.accountId(), issued.partnerId(), action, issued.email());
			return result(invitation, issued.partnerId(), LocalDateTime.now());
		} catch (RuntimeException exception) {
			tokenService.cancelIssued(issued);
			throw exception;
		}
	}

	private PartnerAccountInvitation validInvitation(String rawToken, boolean forUpdate) {
		String token = requireText(rawToken, "Invitation token is required.");
		String tokenHash = tokenCodec.hash(token);
		PartnerAccountInvitation invitation = forUpdate
			? invitationRepository.findForUpdateByTokenHash(tokenHash).orElseThrow(this::invalidToken)
			: invitationRepository.findByTokenHash(tokenHash).orElseThrow(this::invalidToken);
		LocalDateTime now = LocalDateTime.now();
		if (invitation.status() != PartnerAccountInvitationStatus.PENDING
			|| invitation.isExpired(now)
			|| invitation.partner().deletedAt() != null
			|| invitation.partner().status() == PartnerStatus.WITHDRAWN) {
			throw invalidToken();
		}
		return invitation;
	}

	private Partner findPartner(Long partnerId) {
		return partnerRepository.findByIdAndDeletedAtIsNull(partnerId)
			.orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Partner not found."));
	}

	private PartnerAccountInvitationResult result(
		PartnerAccountInvitation invitation,
		LocalDateTime now
	) {
		return result(invitation, invitation.partnerId(), now);
	}

	private PartnerAccountInvitationResult result(
		PartnerAccountInvitation invitation,
		Long partnerId,
		LocalDateTime now
	) {
		return new PartnerAccountInvitationResult(
			invitation.id(),
			partnerId,
			invitation.email(),
			invitation.recipientName(),
			invitation.effectiveStatus(now).name(),
			invitation.expiresAt(),
			invitation.sentAt(),
			invitation.acceptedAt(),
			invitation.canceledAt(),
			invitation.createdByStaffId(),
			invitation.createdAt(),
			invitation.updatedAt()
		);
	}

	private void recordHistory(
		String actorType,
		Long actorId,
		Long partnerId,
		String action,
		String email
	) {
		historyRepository.save(new OperationHistory(
			OperationHistory.TARGET_PARTNER,
			partnerId,
			actorType,
			actorId,
			action,
			null,
			email
		));
	}

	private String setupUrl(String rawToken) {
		URI uri = UriComponentsBuilder.fromUriString(properties.frontendUrl())
			.queryParam("token", rawToken)
			.build()
			.encode()
			.toUri();
		return uri.toString();
	}

	private String maskEmail(String email) {
		int at = email.indexOf('@');
		if (at <= 1) {
			return "*" + email.substring(Math.max(at, 0));
		}
		return email.charAt(0) + "***" + email.substring(at);
	}

	private void validatePassword(String password) {
		if (password.length() < 8 || password.getBytes(StandardCharsets.UTF_8).length > 72) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "Password must be 8 to 72 bytes.");
		}
	}

	private String normalizeEmail(String value) {
		return requireText(value, "Email is required.").toLowerCase(Locale.ROOT);
	}

	private String requireText(String value, String message) {
		String normalized = trimToNull(value);
		if (normalized == null) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, message);
		}
		return normalized;
	}

	private String trimToNull(String value) {
		if (value == null) {
			return null;
		}
		String normalized = value.trim();
		return normalized.isEmpty() ? null : normalized;
	}

	private ApiException invalidToken() {
		return new ApiException(ErrorCode.TOKEN_ERROR, "Invitation link is invalid or expired.");
	}
}
