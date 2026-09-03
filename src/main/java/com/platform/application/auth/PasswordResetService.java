package com.platform.application.auth;

import com.platform.application.auth.command.PasswordResetCommand;
import com.platform.application.auth.command.AuthClientContext;
import com.platform.application.auth.command.PasswordResetLinkCommand;
import com.platform.application.auth.command.PasswordResetTokenVerifyCommand;
import com.platform.application.auth.result.PasswordResetMessageResult;
import com.platform.application.auth.result.PasswordResetTokenVerifyResult;
import com.platform.common.error.ApiException;
import com.platform.common.error.ErrorCode;
import com.platform.domain.account.AccountActorType;
import com.platform.domain.account.AccountPartnerStatus;
import com.platform.domain.account.AccountStaff;
import com.platform.domain.account.AccountUser;
import com.platform.domain.auth.PasswordResetToken;
import com.platform.domain.operationhistory.OperationHistory;
import com.platform.infrastructure.persistence.operationhistory.OperationHistoryRepository;
import com.platform.infrastructure.persistence.account.AccountPartnerRepository;
import com.platform.infrastructure.persistence.account.AccountStaffRepository;
import com.platform.infrastructure.persistence.account.AccountUserRepository;
import com.platform.infrastructure.persistence.auth.PasswordResetTokenRepository;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PasswordResetService {

	private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);
	private static final String GENERIC_MESSAGE = "비밀번호 재설정 링크가 발송되었습니다.";

	private final AccountStaffRepository staffRepository;
	private final AccountPartnerRepository partnerRepository;
	private final AccountUserRepository userRepository;
	private final PasswordResetTokenRepository tokenRepository;
	private final PasswordResetTokenService tokenService;
	private final PasswordResetRateLimitPolicy rateLimitPolicy;
	private final PasswordResetDeliveryService deliveryService;
	private final OperationHistoryRepository historyRepository;
	private final AuthSessionService authSessionService;
	private final PasswordEncoder passwordEncoder;

	public PasswordResetService(
		AccountStaffRepository staffRepository,
		AccountPartnerRepository partnerRepository,
		AccountUserRepository userRepository,
		PasswordResetTokenRepository tokenRepository,
		PasswordResetTokenService tokenService,
		PasswordResetRateLimitPolicy rateLimitPolicy,
		PasswordResetDeliveryService deliveryService,
		OperationHistoryRepository historyRepository,
		AuthSessionService authSessionService,
		PasswordEncoder passwordEncoder
	) {
		this.staffRepository = staffRepository;
		this.partnerRepository = partnerRepository;
		this.userRepository = userRepository;
		this.tokenRepository = tokenRepository;
		this.tokenService = tokenService;
		this.rateLimitPolicy = rateLimitPolicy;
		this.deliveryService = deliveryService;
		this.historyRepository = historyRepository;
		this.authSessionService = authSessionService;
		this.passwordEncoder = passwordEncoder;
	}

	public PasswordResetMessageResult sendLink(AccountActorType actorType, PasswordResetLinkCommand command) {
		String email = normalizeEmail(command.email());
		rateLimitPolicy.checkLinkRequest(actorType, email, command.client().ipAddress());
		Optional<AccountHandle> account = resettableAccount(actorType, email);
		if (account.isEmpty()) {
			log.info("비밀번호 재설정 링크 요청 대상 없음 actor={}, emailHash={}", actorType, tokenService.hash(email));
			return new PasswordResetMessageResult(GENERIC_MESSAGE);
		}

		try {
			if (deliveryService.send(actorType, email, email)) {
				log.info("비밀번호 재설정 링크 발송 actor={}, accountId={}", actorType, account.get().id());
			}
		} catch (RuntimeException exception) {
			log.error("비밀번호 재설정 링크 발송 실패 actor={}, accountId={}", actorType, account.get().id(), exception);
		}

		return new PasswordResetMessageResult(GENERIC_MESSAGE);
	}

	public PasswordResetMessageResult sendLinkForStaff(
		AccountActorType actorType,
		String targetEmail,
		AuthClientContext client
	) {
		return sendLinkForStaff(actorType, targetEmail, targetEmail, client);
	}

	public PasswordResetMessageResult sendLinkForStaff(
		AccountActorType actorType,
		String accountEmail,
		String recipientEmail,
		AuthClientContext client
	) {
		String email = normalizeEmail(accountEmail);
		String recipient = normalizeEmail(recipientEmail);
		rateLimitPolicy.checkLinkRequest(actorType, email, client.ipAddress());
		AccountHandle account = resettableAccount(actorType, email)
			.orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "비밀번호를 재설정할 계정을 찾을 수 없습니다."));
		try {
			if (!deliveryService.send(actorType, email, recipient)) {
				throw new ApiException(ErrorCode.RATE_LIMITED, "최근 발송된 링크가 있습니다. 잠시 후 다시 시도해주세요.");
			}
			log.info("관리자 비밀번호 재설정 링크 발송 actor={}, accountId={}", actorType, account.id());
		} catch (ApiException exception) {
			throw exception;
		} catch (RuntimeException exception) {
			log.error("관리자 비밀번호 재설정 링크 발송 실패 actor={}, accountId={}", actorType, account.id(), exception);
			throw new ApiException(ErrorCode.SERVICE_UNAVAILABLE, "비밀번호 재설정 메일 발송에 실패했습니다.");
		}
		return new PasswordResetMessageResult(GENERIC_MESSAGE);
	}

	@Transactional(readOnly = true)
	public PasswordResetTokenVerifyResult verify(
		AccountActorType actorType,
		PasswordResetTokenVerifyCommand command
	) {
		String email = normalizeEmail(command.email());
		rateLimitPolicy.checkTokenVerification(actorType, email, command.client().ipAddress());
		if (resettableAccount(actorType, email).isEmpty() || !tokenService.isValid(actorType, email, command.token())) {
			throw invalidToken();
		}
		return new PasswordResetTokenVerifyResult(true);
	}

	@Transactional
	public PasswordResetMessageResult reset(AccountActorType actorType, PasswordResetCommand command) {
		String email = normalizeEmail(command.email());
		rateLimitPolicy.checkPasswordReset(actorType, email, command.client().ipAddress());
		PasswordResetToken token = tokenRepository.findForUpdate(actorType, email).orElseThrow(this::invalidToken);
		if (token.isExpired(LocalDateTime.now())
			|| !tokenService.constantTimeEquals(token.tokenHash(), tokenService.hash(command.token()))) {
			throw invalidToken();
		}

		AccountHandle account = resettableAccount(actorType, email).orElseThrow(this::invalidToken);
		validateNewPassword(command.password(), account.encodedPassword());
		account.passwordUpdater().accept(passwordEncoder.encode(command.password()));
		tokenRepository.delete(token);
		authSessionService.revokeAll(actorType, account.id(), "PASSWORD_RESET");
		historyRepository.save(new OperationHistory(actorType.name() + "_ACCOUNT", account.id(),
			actorType.name(), account.id(), "PASSWORD_RESET_COMPLETED", null, null)
			.captureActor(account.name(), account.loginId())
			.captureRequest(command.client().ipAddress(), command.client().userAgent()));
		log.info("비밀번호 재설정 완료 actor={}, accountId={}", actorType, account.id());
		return new PasswordResetMessageResult("비밀번호가 변경되었습니다.");
	}

	private Optional<AccountHandle> resettableAccount(AccountActorType actorType, String email) {
		return switch (actorType) {
			case STAFF -> staffRepository.findByEmailAndDeletedAtIsNull(email)
				.filter(AccountStaff::isActive)
				.map(account -> new AccountHandle(account.id(), account.name(), account.loginId(), account.password(), account::changePassword));
			case PARTNER -> partnerRepository.findByEmailAndDeletedAtIsNull(email)
				.filter(account -> account.status() != AccountPartnerStatus.WITHDRAWN)
				.map(account -> new AccountHandle(account.id(), account.name(), account.loginId(), account.password(), account::changePassword));
			case USER -> userRepository.findByEmailAndDeletedAtIsNull(email)
				.filter(AccountUser::isActive)
				.map(account -> new AccountHandle(account.id(), account.name(), account.email(), account.password(), account::changePassword));
		};
	}

	private void validateNewPassword(String password, String currentEncodedPassword) {
		if (password == null || password.length() < 8) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "비밀번호는 8자 이상 입력해주세요.");
		}
		if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "비밀번호는 UTF-8 기준 72바이트 이하여야 합니다.");
		}
		if (passwordEncoder.matches(password, currentEncodedPassword)) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "기존 비밀번호와 다른 비밀번호를 입력해주세요.");
		}
	}

	private String normalizeEmail(String value) {
		if (value == null || value.isBlank()) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "이메일을 입력해주세요.");
		}
		return value.trim().toLowerCase(Locale.ROOT);
	}

	private ApiException invalidToken() {
		return new ApiException(ErrorCode.TOKEN_ERROR, "비밀번호 재설정 링크가 유효하지 않거나 만료되었습니다.");
	}

	private record AccountHandle(Long id, String name, String loginId, String encodedPassword, Consumer<String> passwordUpdater) {
	}
}
