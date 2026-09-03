package com.platform.application.account;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.platform.application.auth.AuthSessionService;
import com.platform.application.auth.LoginAttemptPolicy;
import com.platform.application.auth.PasswordResetService;
import com.platform.application.auth.PermissionService;
import com.platform.application.auth.command.AuthClientContext;
import com.platform.application.auth.result.PasswordResetMessageResult;
import com.platform.application.auth.result.LoginAttemptStatusResult;
import com.platform.application.partner.command.ChangePartnerAccountStatusCommand;
import com.platform.common.security.AuthenticatedActor;
import com.platform.common.error.ApiException;
import com.platform.domain.account.AccountActorType;
import com.platform.domain.account.AccountPartner;
import com.platform.domain.account.AccountPartnerStatus;
import com.platform.domain.operationhistory.OperationHistory;
import com.platform.domain.auth.AuthenticationEvent;
import com.platform.domain.auth.AuthenticationEventResult;
import com.platform.infrastructure.persistence.account.AccountPartnerRepository;
import com.platform.infrastructure.persistence.auth.AuthenticationEventRepository;
import com.platform.infrastructure.persistence.operationhistory.OperationHistoryRepository;
import com.platform.infrastructure.persistence.partner.PartnerMembershipRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PartnerAccountForStaffServiceTests {

	@Mock
	private PermissionService permissionService;
	@Mock
	private AuthSessionService authSessionService;
	@Mock
	private LoginAttemptPolicy loginAttemptPolicy;
	@Mock
	private PasswordResetService passwordResetService;
	@Mock
	private AccountPartnerRepository accountRepository;
	@Mock
	private PartnerMembershipRepository membershipRepository;
	@Mock
	private OperationHistoryRepository operationHistoryRepository;
	@Mock
	private AuthenticationEventRepository authenticationEventRepository;

	private PartnerAccountForStaffService service;
	private AuthenticatedActor actor;

	@BeforeEach
	void setUp() {
		service = new PartnerAccountForStaffService(
			permissionService,
			authSessionService,
			loginAttemptPolicy,
			passwordResetService,
			accountRepository,
			membershipRepository,
			operationHistoryRepository,
			authenticationEventRepository
		);
		actor = mock(AuthenticatedActor.class);
	}

	@Test
	void sendsPasswordResetLinkAndRecordsAccountHistory() {
		AccountPartner account = account(9L, AccountPartnerStatus.ACTIVE);
		AuthClientContext client = new AuthClientContext("127.0.0.1", "test");
		when(actor.actorType()).thenReturn(AccountActorType.STAFF);
		when(actor.accountId()).thenReturn(100L);
		when(accountRepository.findByIdAndDeletedAtIsNull(9L)).thenReturn(Optional.of(account));
		when(passwordResetService.sendLinkForStaff(
			AccountActorType.PARTNER,
			account.email(),
			"temporary@platform.local",
			client
		))
			.thenReturn(new PasswordResetMessageResult("비밀번호 재설정 링크가 발송되었습니다."));

		var result = service.sendPasswordResetLink(actor, 9L, "Temporary@Platform.Local", client);

		assertEquals("비밀번호 재설정 링크가 발송되었습니다.", result.message());
		verify(passwordResetService).sendLinkForStaff(
			AccountActorType.PARTNER,
			account.email(),
			"temporary@platform.local",
			client
		);
		ArgumentCaptor<OperationHistory> historyCaptor = ArgumentCaptor.forClass(OperationHistory.class);
		verify(operationHistoryRepository).save(historyCaptor.capture());
		assertEquals(OperationHistory.TARGET_PARTNER_ACCOUNT, historyCaptor.getValue().targetType());
		assertEquals(9L, historyCaptor.getValue().targetId());
		assertEquals("PARTNER_ACCOUNT_PASSWORD_RESET_LINK_SENT", historyCaptor.getValue().action());
		assertEquals("recipient_email", historyCaptor.getValue().changes().getFirst().fieldKey());
		assertEquals("temporary@platform.local", historyCaptor.getValue().changes().getFirst().afterValue());
	}

	@Test
	void doesNotSendPasswordResetLinkToWithdrawnAccount() {
		AccountPartner account = account(17L, AccountPartnerStatus.WITHDRAWN);
		when(accountRepository.findByIdAndDeletedAtIsNull(17L)).thenReturn(Optional.of(account));

		assertThrows(
			ApiException.class,
			() -> service.sendPasswordResetLink(
				actor,
				17L,
				"temporary@platform.local",
				new AuthClientContext("127.0.0.1", "test")
			)
		);

		verify(passwordResetService, never()).sendLinkForStaff(
			org.mockito.ArgumentMatchers.any(),
			org.mockito.ArgumentMatchers.anyString(),
			org.mockito.ArgumentMatchers.anyString(),
			org.mockito.ArgumentMatchers.any()
		);
	}

	@Test
	void getsPartnerAccountDetailWithManagedBusinesses() {
		AccountPartner account = account(6L, AccountPartnerStatus.ACTIVE);
		when(accountRepository.findByIdAndDeletedAtIsNull(6L)).thenReturn(Optional.of(account));
		when(membershipRepository.findAllForAccountIds(List.of(6L), com.platform.domain.partner.PartnerMembershipStatus.ACTIVE))
			.thenReturn(List.of());

		var result = service.get(actor, 6L);

		assertEquals(6L, result.id());
		assertEquals("파트너 6", result.name());
		assertEquals("partner_6", result.loginId());
		assertEquals(0, result.managedPartnerCount());
	}

	@Test
	void includesActionsPerformedByPartnerInAccountHistory() {
		AccountPartner account = account(10L, AccountPartnerStatus.ACTIVE);
		OperationHistory partnerAction = new OperationHistory(
			OperationHistory.TARGET_PARTNER,
			99L,
			AccountActorType.PARTNER.name(),
			10L,
			"ONBOARDING_SUBMITTED",
			null,
			null
		);
		ReflectionTestUtils.setField(partnerAction, "id", 501L);
		when(accountRepository.findByIdAndDeletedAtIsNull(10L)).thenReturn(Optional.of(account));
		when(operationHistoryRepository.findAllForPartnerAccountHistory(
			org.mockito.ArgumentMatchers.eq(OperationHistory.TARGET_PARTNER_ACCOUNT),
			org.mockito.ArgumentMatchers.eq(10L),
			org.mockito.ArgumentMatchers.eq(AccountActorType.PARTNER.name()),
			org.mockito.ArgumentMatchers.any(Pageable.class)
		)).thenReturn(new PageImpl<>(List.of(partnerAction)));
		when(operationHistoryRepository.findWithChangesByIdIn(List.of(501L)))
			.thenReturn(List.of(partnerAction));

		var result = service.managementHistories(actor, 10L, 1, 10);
		var item = result.items().getFirst();

		assertEquals(OperationHistory.TARGET_PARTNER, item.targetType());
		assertEquals(99L, item.targetId());
		assertEquals(AccountActorType.PARTNER.name(), item.actorType());
		assertEquals("파트너 10", item.actorName());
		assertEquals("partner_10", item.actorLoginId());
	}

	@Test
	void blockingAccountRevokesSessionsAndRecordsAccountHistory() {
		AccountPartner account = account(7L, AccountPartnerStatus.ACTIVE);
		when(actor.actorType()).thenReturn(AccountActorType.STAFF);
		when(actor.accountId()).thenReturn(100L);
		when(accountRepository.findForUpdateByIdAndDeletedAtIsNull(7L)).thenReturn(Optional.of(account));
		when(membershipRepository.findAllForAccountIds(List.of(7L), com.platform.domain.partner.PartnerMembershipStatus.ACTIVE))
			.thenReturn(List.of());

		var result = service.changeStatus(
			actor,
			7L,
			new ChangePartnerAccountStatusCommand(AccountPartnerStatus.BLOCKED, null),
			new AuthClientContext("127.0.0.1", "test")
		);

		assertEquals("BLOCKED", result.status());
		verify(authSessionService).revokeAll(AccountActorType.PARTNER, 7L, "PARTNER_ACCOUNT_BLOCKED");
		ArgumentCaptor<OperationHistory> historyCaptor = ArgumentCaptor.forClass(OperationHistory.class);
		verify(operationHistoryRepository).save(historyCaptor.capture());
		assertEquals(OperationHistory.TARGET_PARTNER_ACCOUNT, historyCaptor.getValue().targetType());
		assertEquals(7L, historyCaptor.getValue().targetId());
	}

	@Test
	void unchangedStatusDoesNotRevokeSessionsOrWriteHistory() {
		AccountPartner account = account(8L, AccountPartnerStatus.ACTIVE);
		when(accountRepository.findForUpdateByIdAndDeletedAtIsNull(8L)).thenReturn(Optional.of(account));
		when(membershipRepository.findAllForAccountIds(List.of(8L), com.platform.domain.partner.PartnerMembershipStatus.ACTIVE))
			.thenReturn(List.of());

		service.changeStatus(
			actor,
			8L,
			new ChangePartnerAccountStatusCommand(AccountPartnerStatus.ACTIVE, null),
			new AuthClientContext("127.0.0.1", "test")
		);

		verify(authSessionService, never()).revokeAll(
			AccountActorType.PARTNER,
			8L,
			"PARTNER_ACCOUNT_BLOCKED"
		);
		verify(operationHistoryRepository, never()).save(org.mockito.ArgumentMatchers.any());
	}

	@Test
	void staffCannotWithdrawOrRestorePartnerAccount() {
		AccountPartner activeAccount = account(18L, AccountPartnerStatus.ACTIVE);
		when(accountRepository.findForUpdateByIdAndDeletedAtIsNull(18L)).thenReturn(Optional.of(activeAccount));

		assertThrows(
			ApiException.class,
			() -> service.changeStatus(
				actor,
				18L,
				new ChangePartnerAccountStatusCommand(AccountPartnerStatus.WITHDRAWN, null),
				new AuthClientContext("127.0.0.1", "test")
			)
		);

		AccountPartner withdrawnAccount = account(19L, AccountPartnerStatus.WITHDRAWN);
		when(accountRepository.findForUpdateByIdAndDeletedAtIsNull(19L)).thenReturn(Optional.of(withdrawnAccount));

		assertThrows(
			ApiException.class,
			() -> service.changeStatus(
				actor,
				19L,
				new ChangePartnerAccountStatusCommand(AccountPartnerStatus.ACTIVE, null),
				new AuthClientContext("127.0.0.1", "test")
			)
		);
	}

	@Test
	void readsTemporaryLoginLockStatus() {
		AccountPartner account = account(10L, AccountPartnerStatus.ACTIVE);
		when(accountRepository.findByIdAndDeletedAtIsNull(10L)).thenReturn(Optional.of(account));
		when(loginAttemptPolicy.status(AccountActorType.PARTNER, account.loginId()))
			.thenReturn(new LoginAttemptStatusResult(4, false, null));
		when(authSessionService.activeSessions(AccountActorType.PARTNER, 10L)).thenReturn(List.of());

		var result = service.security(actor, 10L);

		assertEquals(4, result.failureCount());
		assertFalse(result.locked());
		assertEquals(0, result.activeSessions().size());
	}

	@Test
	void clearsLoginLockAndRecordsManagementHistory() {
		AccountPartner account = account(11L, AccountPartnerStatus.ACTIVE);
		when(actor.actorType()).thenReturn(AccountActorType.STAFF);
		when(actor.accountId()).thenReturn(100L);
		when(accountRepository.findByIdAndDeletedAtIsNull(11L)).thenReturn(Optional.of(account));
		when(loginAttemptPolicy.clear(AccountActorType.PARTNER, account.loginId()))
			.thenReturn(new LoginAttemptStatusResult(15, true, java.time.LocalDateTime.now().plusMinutes(5)));
		when(loginAttemptPolicy.status(AccountActorType.PARTNER, account.loginId()))
			.thenReturn(LoginAttemptStatusResult.unlocked());
		when(authSessionService.activeSessions(AccountActorType.PARTNER, 11L)).thenReturn(List.of());

		service.clearLoginLock(actor, 11L, new AuthClientContext("127.0.0.1", "test"));

		ArgumentCaptor<OperationHistory> historyCaptor = ArgumentCaptor.forClass(OperationHistory.class);
		verify(operationHistoryRepository).save(historyCaptor.capture());
		assertEquals("PARTNER_ACCOUNT_LOGIN_LOCK_CLEARED", historyCaptor.getValue().action());
		assertEquals("login_lock", historyCaptor.getValue().changes().getFirst().fieldKey());
	}

	@Test
	void returnsPaginatedAccessEvents() {
		AccountPartner account = account(12L, AccountPartnerStatus.ACTIVE);
		AuthenticationEvent event = AuthenticationEvent.create(
			AccountActorType.PARTNER,
			12L,
			AuthenticationEventResult.FAILURE,
			"UNAUTHORIZED",
			"127.0.0.1",
			"test"
		);
		ReflectionTestUtils.setField(event, "id", 1L);
		ReflectionTestUtils.setField(event, "createdAt", java.time.LocalDateTime.now());
		when(accountRepository.findByIdAndDeletedAtIsNull(12L)).thenReturn(Optional.of(account));
		when(authenticationEventRepository.findByActorTypeAndAccountId(
			org.mockito.ArgumentMatchers.eq(AccountActorType.PARTNER),
			org.mockito.ArgumentMatchers.eq(12L),
			org.mockito.ArgumentMatchers.any(Pageable.class)
		)).thenReturn(new PageImpl<>(List.of(event)));

		var result = service.accessEvents(actor, 12L, 1, 10);

		assertEquals(1, result.items().size());
		assertEquals("FAILURE", result.items().getFirst().result());
		assertEquals("UNAUTHORIZED", result.items().getFirst().failureCode());
	}

	private AccountPartner account(Long id, AccountPartnerStatus status) {
		AccountPartner account = AccountPartner.create(
			"파트너 " + id,
			"partner_" + id,
			"partner_" + id + "@platform.local",
			"01012345678",
			"encoded-password",
			status
		);
		ReflectionTestUtils.setField(account, "id", id);
		return account;
	}
}
