package com.platform.application.account;

import com.platform.application.account.query.SearchPartnerAccountsForStaffQuery;
import com.platform.application.account.result.PartnerAccountBusinessForStaffResult;
import com.platform.application.account.result.PartnerAccountAccessEventForStaffResult;
import com.platform.application.account.result.PartnerAccountListItemForStaffResult;
import com.platform.application.account.result.PartnerAccountManagementHistoryChangeForStaffResult;
import com.platform.application.account.result.PartnerAccountManagementHistoryForStaffResult;
import com.platform.application.account.result.PartnerAccountSecurityForStaffResult;
import com.platform.application.auth.AuthSessionService;
import com.platform.application.auth.LoginAttemptPolicy;
import com.platform.application.auth.PasswordResetService;
import com.platform.application.auth.PermissionService;
import com.platform.application.auth.command.AuthClientContext;
import com.platform.application.auth.result.PasswordResetMessageResult;
import com.platform.application.partner.command.ChangePartnerAccountStatusCommand;
import com.platform.common.error.ApiException;
import com.platform.common.error.ErrorCode;
import com.platform.common.security.AccessPermissions;
import com.platform.common.security.AuthenticatedActor;
import com.platform.common.web.PaginatedResponse;
import com.platform.domain.account.AccountActorType;
import com.platform.domain.account.AccountPartner;
import com.platform.domain.account.AccountPartnerStatus;
import com.platform.domain.operationhistory.OperationHistory;
import com.platform.domain.auth.AuthenticationEvent;
import com.platform.domain.partner.PartnerMembership;
import com.platform.domain.partner.PartnerMembershipStatus;
import com.platform.infrastructure.persistence.account.AccountPartnerRepository;
import com.platform.infrastructure.persistence.auth.AuthenticationEventRepository;
import com.platform.infrastructure.persistence.operationhistory.OperationHistoryRepository;
import com.platform.infrastructure.persistence.partner.PartnerMembershipRepository;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class PartnerAccountForStaffService {

	private static final String ACTION_STATUS_UPDATED = "PARTNER_ACCOUNT_STATUS_UPDATED";
	private static final String ACTION_PASSWORD_RESET_LINK_SENT = "PARTNER_ACCOUNT_PASSWORD_RESET_LINK_SENT";
	private static final String ACTION_LOGIN_LOCK_CLEARED = "PARTNER_ACCOUNT_LOGIN_LOCK_CLEARED";
	private static final String ACTION_SESSION_REVOKED = "PARTNER_ACCOUNT_SESSION_REVOKED";
	private static final int DORMANT_DAYS = 30;

	private final PermissionService permissionService;
	private final AuthSessionService authSessionService;
	private final LoginAttemptPolicy loginAttemptPolicy;
	private final PasswordResetService passwordResetService;
	private final AccountPartnerRepository accountRepository;
	private final PartnerMembershipRepository membershipRepository;
	private final OperationHistoryRepository operationHistoryRepository;
	private final AuthenticationEventRepository authenticationEventRepository;

	public PartnerAccountForStaffService(
		PermissionService permissionService,
		AuthSessionService authSessionService,
		LoginAttemptPolicy loginAttemptPolicy,
		PasswordResetService passwordResetService,
		AccountPartnerRepository accountRepository,
		PartnerMembershipRepository membershipRepository,
		OperationHistoryRepository operationHistoryRepository,
		AuthenticationEventRepository authenticationEventRepository
	) {
		this.permissionService = permissionService;
		this.authSessionService = authSessionService;
		this.loginAttemptPolicy = loginAttemptPolicy;
		this.passwordResetService = passwordResetService;
		this.accountRepository = accountRepository;
		this.membershipRepository = membershipRepository;
		this.operationHistoryRepository = operationHistoryRepository;
		this.authenticationEventRepository = authenticationEventRepository;
	}

	@Transactional(readOnly = true)
	public PaginatedResponse<PartnerAccountListItemForStaffResult> list(
		AuthenticatedActor actor,
		SearchPartnerAccountsForStaffQuery condition
	) {
		permissionService.requireStaffPermission(actor, AccessPermissions.PARTNER_SHOW);
		Pageable pageable = PageRequest.of(
			Math.max(condition.page(), 1) - 1,
			Math.clamp(condition.perPage(), 1, 100),
			sort(condition)
		);
		Page<AccountPartner> page = accountRepository.findAll(specification(condition), pageable);
		Map<Long, List<PartnerAccountBusinessForStaffResult>> businesses = businessesByAccountIds(
			page.getContent().stream().map(AccountPartner::id).toList()
		);

		return PaginatedResponse.from(page, account -> toListItem(
			account,
			businesses.getOrDefault(account.id(), List.of())
		));
	}

	@Transactional(readOnly = true)
	public PartnerAccountListItemForStaffResult get(AuthenticatedActor actor, Long accountId) {
		permissionService.requireStaffPermission(actor, AccessPermissions.PARTNER_SHOW);
		AccountPartner account = accountRepository.findByIdAndDeletedAtIsNull(accountId)
			.orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "파트너 계정을 찾을 수 없습니다."));
		List<PartnerAccountBusinessForStaffResult> businesses = businessesByAccountIds(List.of(account.id()))
			.getOrDefault(account.id(), List.of());
		return toListItem(account, businesses);
	}

	@Transactional
	public PartnerAccountListItemForStaffResult changeStatus(
		AuthenticatedActor actor,
		Long accountId,
		ChangePartnerAccountStatusCommand command,
		AuthClientContext client
	) {
		permissionService.requireStaffPermission(actor, AccessPermissions.PARTNER_ACCOUNT_STATUS_UPDATE);
		AccountPartner account = accountRepository.findForUpdateByIdAndDeletedAtIsNull(accountId)
			.orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "파트너 계정을 찾을 수 없습니다."));
		AccountPartnerStatus before = account.status();
		assertAccountStatusTransition(before, command.status());
		if (before != command.status()) {
			account.changeStatus(command.status());
			accountRepository.saveAndFlush(account);
			if (account.status() == AccountPartnerStatus.BLOCKED) {
				authSessionService.revokeAll(AccountActorType.PARTNER, account.id(), "PARTNER_ACCOUNT_BLOCKED");
			}
			recordStatusHistory(actor, account, before, command, client);
		}

		List<PartnerAccountBusinessForStaffResult> businesses = businessesByAccountIds(List.of(account.id()))
			.getOrDefault(account.id(), List.of());
		return toListItem(account, businesses);
	}

	private void assertAccountStatusTransition(AccountPartnerStatus before, AccountPartnerStatus after) {
		if (after == null) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "계정상태를 입력해주세요.");
		}
		if (!after.staffSelectable() && before != after) {
			throw new ApiException(
				ErrorCode.INVALID_REQUEST,
				"탈퇴는 계정상태 변경으로 처리할 수 없습니다."
			);
		}
		if (before == AccountPartnerStatus.WITHDRAWN && after != AccountPartnerStatus.WITHDRAWN) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "탈퇴한 계정은 상태를 변경할 수 없습니다.");
		}
	}

	@Transactional
	public PasswordResetMessageResult sendPasswordResetLink(
		AuthenticatedActor actor,
		Long accountId,
		String recipientEmail,
		AuthClientContext client
	) {
		permissionService.requireStaffPermission(actor, AccessPermissions.PARTNER_ACCOUNT_PASSWORD_RESET);
		AccountPartner account = accountRepository.findByIdAndDeletedAtIsNull(accountId)
			.orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "파트너 계정을 찾을 수 없습니다."));
		if (account.status() == AccountPartnerStatus.WITHDRAWN) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "탈퇴한 계정에는 비밀번호 재설정 링크를 보낼 수 없습니다.");
		}
		String normalizedRecipientEmail = recipientEmail.trim().toLowerCase(Locale.ROOT);
		PasswordResetMessageResult result = passwordResetService.sendLinkForStaff(
			AccountActorType.PARTNER,
			account.email(),
			normalizedRecipientEmail,
			client
		);
		OperationHistory history = history(
			actor,
			client,
			OperationHistory.TARGET_PARTNER_ACCOUNT,
			account.id(),
			ACTION_PASSWORD_RESET_LINK_SENT,
			null,
			null
		);
		history.addChange("recipient_email", null, normalizedRecipientEmail);
		operationHistoryRepository.save(history);
		return result;
	}

	@Transactional(readOnly = true)
	public PartnerAccountSecurityForStaffResult security(AuthenticatedActor actor, Long accountId) {
		permissionService.requireStaffPermission(actor, AccessPermissions.PARTNER_SHOW);
		AccountPartner account = account(accountId);
		var lock = loginAttemptPolicy.status(AccountActorType.PARTNER, account.loginId());
		return new PartnerAccountSecurityForStaffResult(
			lock.failureCount(),
			lock.locked(),
			lock.lockedUntil(),
			authSessionService.activeSessions(AccountActorType.PARTNER, account.id())
		);
	}

	@Transactional
	public PartnerAccountSecurityForStaffResult clearLoginLock(
		AuthenticatedActor actor,
		Long accountId,
		AuthClientContext client
	) {
		permissionService.requireStaffPermission(actor, AccessPermissions.PARTNER_ACCOUNT_SECURITY_UPDATE);
		AccountPartner account = account(accountId);
		var previous = loginAttemptPolicy.clear(AccountActorType.PARTNER, account.loginId());
		if (previous.failureCount() > 0 || previous.locked()) {
			OperationHistory history = history(
				actor,
				client,
				OperationHistory.TARGET_PARTNER_ACCOUNT,
				account.id(),
				ACTION_LOGIN_LOCK_CLEARED,
				null,
				null
			);
			history.addChange("login_lock", previous.locked() ? "LOCKED" : "ATTEMPTS_RECORDED", "UNLOCKED");
			operationHistoryRepository.save(history);
		}
		return security(actor, account.id());
	}

	@Transactional
	public PartnerAccountSecurityForStaffResult revokeSession(
		AuthenticatedActor actor,
		Long accountId,
		String sessionId,
		AuthClientContext client
	) {
		permissionService.requireStaffPermission(actor, AccessPermissions.PARTNER_ACCOUNT_SECURITY_UPDATE);
		AccountPartner account = account(accountId);
		boolean revoked = authSessionService.revokeForStaff(
			sessionId,
			AccountActorType.PARTNER,
			account.id(),
			"REVOKED_BY_STAFF"
		);
		if (!revoked) {
			throw new ApiException(ErrorCode.NOT_FOUND, "종료할 활성 세션을 찾을 수 없습니다.");
		}
		operationHistoryRepository.save(history(
			actor,
			client,
			OperationHistory.TARGET_PARTNER_ACCOUNT,
			account.id(),
			ACTION_SESSION_REVOKED,
			null,
			sessionId
		));
		return security(actor, account.id());
	}


	@Transactional(readOnly = true)
	public PaginatedResponse<PartnerAccountAccessEventForStaffResult> accessEvents(
		AuthenticatedActor actor,
		Long accountId,
		int page,
		int perPage
	) {
		permissionService.requireStaffPermission(actor, AccessPermissions.PARTNER_SHOW);
		account(accountId);
		Page<AuthenticationEvent> events = authenticationEventRepository.findByActorTypeAndAccountId(
			AccountActorType.PARTNER,
			accountId,
			PageRequest.of(
				Math.max(page, 1) - 1,
				Math.clamp(perPage, 1, 50),
				Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id"))
			)
		);
		return PaginatedResponse.from(events, event -> new PartnerAccountAccessEventForStaffResult(
			event.id(),
			event.result().name(),
			event.failureCode(),
			event.ipAddress(),
			event.userAgent(),
			event.createdAt()
		));
	}

	@Transactional(readOnly = true)
	public PaginatedResponse<PartnerAccountManagementHistoryForStaffResult> managementHistories(
		AuthenticatedActor actor,
		Long accountId,
		int page,
		int perPage
	) {
		permissionService.requireStaffPermission(actor, AccessPermissions.PARTNER_SHOW);
		AccountPartner historyAccount = account(accountId);
		Page<OperationHistory> histories = operationHistoryRepository.findAllForPartnerAccountHistory(
			OperationHistory.TARGET_PARTNER_ACCOUNT,
			accountId,
			AccountActorType.PARTNER.name(),
			PageRequest.of(
				Math.max(page, 1) - 1,
				Math.clamp(perPage, 1, 50),
				Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id"))
			)
		);
		Map<Long, OperationHistory> detailed = histories.isEmpty()
			? Map.of()
			: operationHistoryRepository.findWithChangesByIdIn(
				histories.getContent().stream().map(OperationHistory::id).toList()
			).stream().collect(Collectors.toMap(OperationHistory::id, Function.identity()));
		Map<Long, String> partnerNames = businessesByAccountIds(List.of(accountId))
			.getOrDefault(accountId, List.of())
			.stream()
			.collect(Collectors.toMap(
				PartnerAccountBusinessForStaffResult::id,
				PartnerAccountBusinessForStaffResult::name
			));

		return PaginatedResponse.from(histories, item -> managementHistoryResult(
			detailed.getOrDefault(item.id(), item),
			historyAccount,
			partnerNames
		));
	}

	private Specification<AccountPartner> specification(SearchPartnerAccountsForStaffQuery condition) {
		return (root, query, criteriaBuilder) -> {
			List<Predicate> predicates = new ArrayList<>();
			predicates.add(criteriaBuilder.isNull(root.get("deletedAt")));
			String keyword = trimToNull(condition.q());
			if (keyword != null) {
				String pattern = "%" + keyword.toLowerCase(Locale.ROOT) + "%";
				List<Predicate> search = new ArrayList<>();
				search.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), pattern));
				search.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("loginId")), pattern));
				search.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("email")), pattern));
				search.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("phone")), pattern));
				try {
					search.add(criteriaBuilder.equal(root.get("id"), Long.parseLong(keyword)));
				} catch (NumberFormatException ignored) {
					// Search text is not an account ID.
				}
				predicates.add(criteriaBuilder.or(search.toArray(Predicate[]::new)));
			}
			if (condition.status() != null && !condition.status().isEmpty()) {
				predicates.add(root.get("status").in(condition.status()));
			}
			if (Boolean.TRUE.equals(condition.dormant())) {
				LocalDateTime cutoff = LocalDateTime.now().minusDays(DORMANT_DAYS);
				predicates.add(criteriaBuilder.or(
					criteriaBuilder.isNull(root.get("lastLoginAt")),
					criteriaBuilder.lessThan(root.get("lastLoginAt"), cutoff)
				));
			}
			if (StringUtils.hasText(condition.startDate())) {
				predicates.add(criteriaBuilder.greaterThanOrEqualTo(
					root.get("createdAt"),
					LocalDate.parse(condition.startDate()).atStartOfDay()
				));
			}
			if (StringUtils.hasText(condition.endDate())) {
				predicates.add(criteriaBuilder.lessThan(
					root.get("createdAt"),
					LocalDate.parse(condition.endDate()).plusDays(1).atStartOfDay()
				));
			}
			return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
		};
	}

	private Sort sort(SearchPartnerAccountsForStaffQuery condition) {
		String field = trimToNull(condition.sort());
		if (field == null) {
			return Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id"));
		}
		Sort.Direction direction = "asc".equalsIgnoreCase(condition.direction())
			? Sort.Direction.ASC
			: Sort.Direction.DESC;
		return switch (field) {
			case "id" -> Sort.by(direction, "id");
			case "name" -> Sort.by(direction, "name").and(Sort.by(direction, "id"));
			case "login_id" -> Sort.by(direction, "loginId").and(Sort.by(direction, "id"));
			case "status" -> Sort.by(direction, "status").and(Sort.by(direction, "id"));
			case "last_login_at" -> Sort.by(direction, "lastLoginAt").and(Sort.by(direction, "id"));
			case "created_at" -> Sort.by(direction, "createdAt").and(Sort.by(direction, "id"));
			default -> Sort.unsorted();
		};
	}

	private Map<Long, List<PartnerAccountBusinessForStaffResult>> businessesByAccountIds(List<Long> accountIds) {
		if (accountIds.isEmpty()) {
			return Map.of();
		}
		Map<Long, List<PartnerAccountBusinessForStaffResult>> result = new LinkedHashMap<>();
		for (PartnerMembership membership : membershipRepository.findAllForAccountIds(
			accountIds,
			PartnerMembershipStatus.ACTIVE
		)) {
			var partner = membership.partner();
			result.computeIfAbsent(membership.accountPartnerId(), ignored -> new ArrayList<>())
				.add(new PartnerAccountBusinessForStaffResult(
					partner.id(),
					partner.name(),
					partner.status().name(),
					partner.allowStatus().name()
				));
		}
		return result;
	}

	private PartnerAccountListItemForStaffResult toListItem(
		AccountPartner account,
		List<PartnerAccountBusinessForStaffResult> businesses
	) {
		return new PartnerAccountListItemForStaffResult(
			account.id(),
			account.name(),
			account.loginId(),
			account.email(),
			account.phone(),
			account.status().name(),
			account.lastLoginAt(),
			businesses.size(),
			businesses,
			account.createdAt(),
			account.updatedAt()
		);
	}

	private void recordStatusHistory(
		AuthenticatedActor actor,
		AccountPartner account,
		AccountPartnerStatus before,
		ChangePartnerAccountStatusCommand command,
		AuthClientContext client
	) {
		OperationHistory history = history(
			actor,
			client,
			OperationHistory.TARGET_PARTNER_ACCOUNT,
			account.id(),
			ACTION_STATUS_UPDATED,
			trimToNull(command.reason()),
			null
		);
		history.addChange("status", before.name(), account.status().name());
		operationHistoryRepository.save(history);
	}

	private AccountPartner account(Long accountId) {
		return accountRepository.findByIdAndDeletedAtIsNull(accountId)
			.orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "파트너 계정을 찾을 수 없습니다."));
	}

	private OperationHistory history(
		AuthenticatedActor actor,
		AuthClientContext client,
		String targetType,
		Long targetId,
		String action,
		String reason,
		String memo
	) {
		return new OperationHistory(
			targetType,
			targetId,
			actor.actorType().name(),
			actor.accountId(),
			action,
			reason,
			memo
		)
			.captureActor(actor.name(), actor.loginId())
			.captureRequest(client.ipAddress(), client.userAgent());
	}

	private PartnerAccountManagementHistoryForStaffResult managementHistoryResult(
		OperationHistory history,
		AccountPartner account,
		Map<Long, String> partnerNames
	) {
		boolean performedByAccount = AccountActorType.PARTNER.name().equals(history.actorType())
			&& account.id().equals(history.actorId());
		return new PartnerAccountManagementHistoryForStaffResult(
			history.id(),
			history.action(),
			history.reason(),
			history.memo(),
			history.targetType(),
			history.targetId(),
			OperationHistory.TARGET_PARTNER.equals(history.targetType())
				? partnerNames.get(history.targetId())
				: null,
			history.actorType(),
			history.actorId(),
			performedByAccount && !StringUtils.hasText(history.actorNameSnapshot())
				? account.name()
				: history.actorNameSnapshot(),
			performedByAccount && !StringUtils.hasText(history.actorLoginIdSnapshot())
				? account.loginId()
				: history.actorLoginIdSnapshot(),
			history.ipAddress(),
			history.userAgent(),
			history.createdAt(),
			history.changes().stream()
				.map(change -> new PartnerAccountManagementHistoryChangeForStaffResult(
					change.fieldKey(),
					change.beforeValue(),
					change.afterValue()
				))
				.toList()
		);
	}

	private String trimToNull(String value) {
		if (value == null) return null;
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}
}
