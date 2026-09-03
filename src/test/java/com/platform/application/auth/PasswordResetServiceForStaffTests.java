package com.platform.application.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.platform.application.auth.command.AuthClientContext;
import com.platform.application.auth.result.IssuedPasswordResetToken;
import com.platform.common.error.ApiException;
import com.platform.common.error.ErrorCode;
import com.platform.common.security.PasswordResetProperties;
import com.platform.domain.account.AccountActorType;
import com.platform.domain.account.AccountPartner;
import com.platform.domain.account.AccountPartnerStatus;
import com.platform.infrastructure.persistence.account.AccountPartnerRepository;
import com.platform.infrastructure.persistence.account.AccountStaffRepository;
import com.platform.infrastructure.persistence.account.AccountUserRepository;
import com.platform.infrastructure.persistence.auth.PasswordResetTokenRepository;
import com.platform.infrastructure.persistence.operationhistory.OperationHistoryRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceForStaffTests {

	@Mock
	private AccountStaffRepository staffRepository;
	@Mock
	private AccountPartnerRepository partnerRepository;
	@Mock
	private AccountUserRepository userRepository;
	@Mock
	private PasswordResetTokenRepository tokenRepository;
	@Mock
	private PasswordResetTokenService tokenService;
	@Mock
	private PasswordResetRateLimitPolicy rateLimitPolicy;
	@Mock
	private PasswordResetMailSender mailSender;
	@Mock
	private AuthSessionService authSessionService;
	@Mock
	private PasswordEncoder passwordEncoder;

	private PasswordResetService service;
	private AccountPartner account;
	private AuthClientContext client;

	@BeforeEach
	void setUp() {
		PasswordResetProperties properties = new PasswordResetProperties();
		properties.setPartnerUrl("http://localhost:4001/password/reset");
		service = new PasswordResetService(
			staffRepository,
			partnerRepository,
			userRepository,
			tokenRepository,
			tokenService,
			rateLimitPolicy,
			new PasswordResetDeliveryService(tokenService, mailSender, properties),
			mock(OperationHistoryRepository.class),
			authSessionService,
			passwordEncoder
		);
		account = AccountPartner.create(
			"홍길동",
			"partner01",
			"partner01@platform.local",
			"01012345678",
			"encoded-password",
			AccountPartnerStatus.ACTIVE
		);
		ReflectionTestUtils.setField(account, "id", 1L);
		client = new AuthClientContext("127.0.0.1", "test");
		when(partnerRepository.findByEmailAndDeletedAtIsNull(account.email()))
			.thenReturn(Optional.of(account));
		when(tokenService.issue(AccountActorType.PARTNER, account.email()))
			.thenReturn(Optional.of(new IssuedPasswordResetToken("raw-token", "token-hash")));
	}

	@Test
	void sendsPartnerPasswordResetLinkForStaff() {
		var result = service.sendLinkForStaff(AccountActorType.PARTNER, account.email(), client);

		assertEquals("비밀번호 재설정 링크가 발송되었습니다.", result.message());
		verify(mailSender).send(
			eq(AccountActorType.PARTNER),
			eq(account.email()),
			contains("token=raw-token"),
			eq(60L)
		);
	}

	@Test
	void sendsPartnerPasswordResetLinkToStaffSelectedRecipient() {
		var result = service.sendLinkForStaff(
			AccountActorType.PARTNER,
			account.email(),
			"Temporary@Platform.Local",
			client
		);

		assertEquals("비밀번호 재설정 링크가 발송되었습니다.", result.message());
		verify(mailSender).send(
			eq(AccountActorType.PARTNER),
			eq("temporary@platform.local"),
			contains("token=raw-token"),
			eq(60L)
		);
	}

	@Test
	void reportsDeliveryFailureWithoutExplicitlyDeletingTheResetToken() {
		doThrow(new RuntimeException("mail unavailable"))
			.when(mailSender)
			.send(eq(AccountActorType.PARTNER), eq(account.email()), contains("token=raw-token"), eq(60L));

		ApiException exception = assertThrows(
			ApiException.class,
			() -> service.sendLinkForStaff(AccountActorType.PARTNER, account.email(), client)
		);

		assertEquals(ErrorCode.SERVICE_UNAVAILABLE, exception.errorCode());
		verify(tokenService, never()).discard(AccountActorType.PARTNER, account.email(), "token-hash");
	}
}
