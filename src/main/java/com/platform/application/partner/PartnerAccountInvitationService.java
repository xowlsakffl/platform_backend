package com.platform.application.partner;

import com.platform.application.auth.PermissionService;
import com.platform.application.cache.StaffSummaryCache;
import com.platform.application.cache.StaffSummaryCacheInvalidator;
import com.platform.application.partner.command.AcceptPartnerAccountInvitationCommand;
import com.platform.application.partner.command.CreatePartnerAccountInvitationCommand;
import com.platform.application.partner.query.SearchPartnerAccountInvitationsQuery;
import com.platform.application.partner.result.IssuedPartnerAccountInvitation;
import com.platform.application.partner.result.PartnerAccountInvitationAcceptedResult;
import com.platform.application.partner.result.PartnerAccountInvitationListItemResult;
import com.platform.application.partner.result.PartnerAccountInvitationResult;
import com.platform.application.partner.result.PartnerAccountInvitationVerifyResult;
import com.platform.common.error.ApiException;
import com.platform.common.error.ErrorCode;
import com.platform.common.security.AccessPermissions;
import com.platform.common.security.AuthenticatedActor;
import com.platform.common.security.PartnerAccountInvitationProperties;
import com.platform.common.web.PaginatedResponse;
import com.platform.domain.account.AccountActorType;
import com.platform.domain.account.AccountPartner;
import com.platform.domain.account.AccountPartnerStatus;
import com.platform.domain.account.AccountStaff;
import com.platform.domain.operationhistory.OperationHistory;
import com.platform.domain.partner.Partner;
import com.platform.domain.partner.PartnerAccountInvitation;
import com.platform.domain.partner.PartnerAccountInvitationStatus;
import com.platform.domain.partner.PartnerStatus;
import com.platform.infrastructure.persistence.account.AccountPartnerRepository;
import com.platform.infrastructure.persistence.account.AccountStaffRepository;
import com.platform.infrastructure.persistence.operationhistory.OperationHistoryRepository;
import com.platform.infrastructure.persistence.partner.PartnerAccountInvitationRepository;
import com.platform.infrastructure.persistence.partner.PartnerRepository;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class PartnerAccountInvitationService {

	private static final String ACTION_INVITATION_SENT = "ACCOUNT_INVITATION_SENT";
	private static final String ACTION_INVITATION_RESENT = "ACCOUNT_INVITATION_RESENT";
	private static final String ACTION_INVITATION_ACCEPTED = "ACCOUNT_INVITATION_ACCEPTED";

	private final PermissionService permissionService;
	private final PartnerRepository partnerRepository;
	private final AccountPartnerRepository accountPartnerRepository;
	private final AccountStaffRepository accountStaffRepository;
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
		AccountStaffRepository accountStaffRepository,
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
		this.accountStaffRepository = accountStaffRepository;
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
		return invitationRepository.findByPartner_IdOrderByCreatedAtDescIdDesc(partnerId)
			.stream()
			.map(this::result)
			.toList();
	}

	@Transactional(readOnly = true)
	public PaginatedResponse<PartnerAccountInvitationListItemResult> listAll(
		AuthenticatedActor actor,
		SearchPartnerAccountInvitationsQuery condition
	) {
		permissionService.requireStaffPermission(actor, AccessPermissions.PARTNER_SHOW);
		Pageable pageable = PageRequest.of(
			Math.max(condition.page(), 1) - 1,
			Math.min(Math.max(condition.perPage(), 1), 100),
			sort(condition)
		);
		Page<PartnerAccountInvitation> page = invitationRepository.findAll(specification(condition), pageable);
		Map<Long, AccountStaff> staffs = accountStaffRepository.findAllById(
			page.getContent().stream()
				.map(PartnerAccountInvitation::createdByStaffId)
				.filter(Objects::nonNull)
				.distinct()
				.toList()
		).stream().collect(Collectors.toMap(AccountStaff::id, Function.identity()));

		return PaginatedResponse.from(page, invitation -> {
			AccountStaff staff = staffs.get(invitation.createdByStaffId());
			return new PartnerAccountInvitationListItemResult(
				invitation.id(),
				invitation.partnerId(),
				invitation.partner().name(),
				invitation.email(),
				invitation.status().name(),
				invitation.createdAt(),
				invitation.sentAt(),
				invitation.expiresAt(),
				invitation.acceptedAt(),
				invitation.canceledAt(),
				invitation.createdByStaffId(),
				staff == null ? null : staff.name(),
				staff == null ? null : staff.nickname()
			);
		});
	}

	public PartnerAccountInvitationResult invite(
		AuthenticatedActor actor,
		Long partnerId,
		CreatePartnerAccountInvitationCommand command
	) {
		permissionService.requireStaffPermission(actor, AccessPermissions.PARTNER_UPDATE);
		String email = normalizeEmail(command.email());
		IssuedPartnerAccountInvitation issued = tokenService.issue(
			actor.accountId(),
			partnerId,
			email
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
		String loginId = normalizeLoginId(command.loginId());
		String phone = trimToNull(command.phone());
		String password = requireText(command.password(), "Password is required.");
		validatePassword(password);

		if (accountPartnerRepository.findForUpdateByPartner_IdAndDeletedAtIsNull(partner.id()).isPresent()) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "A partner account is already connected.");
		}
		if (accountPartnerRepository.existsByEmail(invitation.email())) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "Email is already in use.");
		}
		if (accountPartnerRepository.existsByLoginId(loginId)) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "Login ID is already in use.");
		}

		AccountPartner account = accountPartnerRepository.saveAndFlush(AccountPartner.create(
			partner,
			loginId,
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
			account.loginId(),
			invitation.email(),
			partner.allowStatus().name(),
			"Partner account has been created."
		);
	}

	private String normalizeLoginId(String value) {
		String loginId = requireText(value, "Login ID is required.").toLowerCase(Locale.ROOT);
		if (!loginId.matches("^[a-z0-9][a-z0-9._-]{3,29}$")) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "Login ID format is invalid.");
		}
		return loginId;
	}

	private PartnerAccountInvitationResult send(
		AuthenticatedActor actor,
		IssuedPartnerAccountInvitation issued,
		String action
	) {
		try {
			mailSender.send(
				issued.email(),
				issued.partnerName(),
				setupUrl(issued.rawToken()),
				Math.max(1, properties.tokenTtlSeconds() / 3_600)
			);
		} catch (RuntimeException exception) {
			tokenService.cancelIssued(issued);
			throw exception;
		}
		PartnerAccountInvitation invitation = tokenService.markSentAndCancelPrevious(issued);
		recordHistory(actor.actorType().name(), actor.accountId(), issued.partnerId(), action, issued.email());
		return result(invitation, issued.partnerId());
	}

	private PartnerAccountInvitation validInvitation(String rawToken, boolean forUpdate) {
		String token = requireText(rawToken, "Invitation token is required.");
		String tokenHash = tokenCodec.hash(token);
		PartnerAccountInvitation invitation = forUpdate
			? invitationRepository.findForUpdateByTokenHash(tokenHash).orElseThrow(this::invalidToken)
			: invitationRepository.findByTokenHash(tokenHash).orElseThrow(this::invalidToken);
		LocalDateTime now = LocalDateTime.now();
		if (invitation.status() != PartnerAccountInvitationStatus.PENDING
			|| invitation.sentAt() == null
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

	private Specification<PartnerAccountInvitation> specification(
		SearchPartnerAccountInvitationsQuery condition
	) {
		return (root, ignored, criteriaBuilder) -> {
			List<Predicate> predicates = new ArrayList<>();
			String keyword = trimToNull(condition.q());
			if (condition.partnerId() != null) {
				predicates.add(criteriaBuilder.equal(root.get("partner").get("id"), condition.partnerId()));
			}

			if (keyword != null) {
				String likeKeyword = "%" + keyword.toLowerCase(Locale.ROOT) + "%";
				List<Predicate> searchPredicates = new ArrayList<>();
				searchPredicates.add(criteriaBuilder.like(
					criteriaBuilder.lower(root.<String>get("email")),
					likeKeyword
				));
				searchPredicates.add(criteriaBuilder.like(
					criteriaBuilder.lower(root.get("partner").<String>get("name")),
					likeKeyword
				));
				try {
					Long id = Long.valueOf(keyword);
					searchPredicates.add(criteriaBuilder.equal(root.get("id"), id));
					searchPredicates.add(criteriaBuilder.equal(root.get("partner").get("id"), id));
				} catch (NumberFormatException ignoredException) {
					// Text search only.
				}
				predicates.add(criteriaBuilder.or(searchPredicates.toArray(Predicate[]::new)));
			}

			if (condition.status() != null && !condition.status().isEmpty()) {
				List<Predicate> statusPredicates = condition.status().stream()
					.map(status -> criteriaBuilder.equal(root.get("status"), status))
					.toList();
				predicates.add(criteriaBuilder.or(statusPredicates.toArray(Predicate[]::new)));
			}

			LocalDateTime start = parseDate(condition.startDate(), false);
			LocalDateTime end = parseDate(condition.endDate(), true);
			if (start != null) {
				predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), start));
			}
			if (end != null) {
				predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), end));
			}
			return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
		};
	}

	private Sort sort(SearchPartnerAccountInvitationsQuery condition) {
		Sort.Direction direction = "asc".equalsIgnoreCase(condition.direction())
			? Sort.Direction.ASC
			: Sort.Direction.DESC;
		String sort = trimToNull(condition.sort());
		if ("expires_at".equals(sort)) {
			return Sort.by(direction, "expiresAt").and(Sort.by(direction, "id"));
		}
		return Sort.by(direction, "createdAt").and(Sort.by(direction, "id"));
	}

	private LocalDateTime parseDate(String value, boolean endOfDay) {
		String normalized = trimToNull(value);
		if (normalized == null) {
			return null;
		}
		LocalDate date = LocalDate.parse(normalized);
		return endOfDay ? date.atTime(LocalTime.MAX) : date.atStartOfDay();
	}

	private PartnerAccountInvitationResult result(PartnerAccountInvitation invitation) {
		return result(invitation, invitation.partnerId());
	}

	private PartnerAccountInvitationResult result(
		PartnerAccountInvitation invitation,
		Long partnerId
	) {
		return new PartnerAccountInvitationResult(
			invitation.id(),
			partnerId,
			invitation.email(),
			invitation.status().name(),
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
