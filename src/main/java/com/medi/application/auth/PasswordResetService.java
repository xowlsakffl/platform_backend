package com.medi.application.auth;

import com.medi.application.auth.command.PasswordResetCommand;
import com.medi.application.auth.command.PasswordResetLinkCommand;
import com.medi.application.auth.command.PasswordResetTokenVerifyCommand;
import com.medi.application.auth.result.IssuedPasswordResetToken;
import com.medi.application.auth.result.PasswordResetMessageResult;
import com.medi.application.auth.result.PasswordResetTokenVerifyResult;
import com.medi.common.error.ApiException;
import com.medi.common.error.ErrorCode;
import com.medi.common.security.PasswordResetProperties;
import com.medi.domain.account.AccountActorType;
import com.medi.domain.account.AccountBeauty;
import com.medi.domain.account.AccountPartner;
import com.medi.domain.account.AccountStaff;
import com.medi.domain.account.AccountUser;
import com.medi.domain.auth.PasswordResetToken;
import com.medi.infrastructure.persistence.account.AccountBeautyRepository;
import com.medi.infrastructure.persistence.account.AccountPartnerRepository;
import com.medi.infrastructure.persistence.account.AccountStaffRepository;
import com.medi.infrastructure.persistence.account.AccountUserRepository;
import com.medi.infrastructure.persistence.auth.PasswordResetTokenRepository;
import java.net.URI;
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
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class PasswordResetService {

	private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);
	private static final String GENERIC_MESSAGE = "비밀번호 재설정 링크가 발송되었습니다.";

	private final AccountStaffRepository staffRepository;
	private final AccountPartnerRepository partnerRepository;
	private final AccountBeautyRepository beautyRepository;
	private final AccountUserRepository userRepository;
	private final PasswordResetTokenRepository tokenRepository;
	private final PasswordResetTokenService tokenService;
	private final PasswordResetRateLimitPolicy rateLimitPolicy;
	private final PasswordResetMailSender mailSender;
	private final AuthSessionService authSessionService;
	private final PasswordEncoder passwordEncoder;
	private final PasswordResetProperties properties;

	public PasswordResetService(
		AccountStaffRepository staffRepository,
		AccountPartnerRepository partnerRepository,
		AccountBeautyRepository beautyRepository,
		AccountUserRepository userRepository,
		PasswordResetTokenRepository tokenRepository,
		PasswordResetTokenService tokenService,
		PasswordResetRateLimitPolicy rateLimitPolicy,
		PasswordResetMailSender mailSender,
		AuthSessionService authSessionService,
		PasswordEncoder passwordEncoder,
		PasswordResetProperties properties
	) {
		this.staffRepository = staffRepository;
		this.partnerRepository = partnerRepository;
		this.beautyRepository = beautyRepository;
		this.userRepository = userRepository;
		this.tokenRepository = tokenRepository;
		this.tokenService = tokenService;
		this.rateLimitPolicy = rateLimitPolicy;
		this.mailSender = mailSender;
		this.authSessionService = authSessionService;
		this.passwordEncoder = passwordEncoder;
		this.properties = properties;
	}

	public PasswordResetMessageResult sendLink(AccountActorType actorType, PasswordResetLinkCommand command) {
		String email = normalizeEmail(command.email());
		rateLimitPolicy.checkLinkRequest(actorType, email, command.client().ipAddress());
		Optional<AccountHandle> account = activeAccount(actorType, email);
		if (account.isEmpty()) {
			log.info("비밀번호 재설정 링크 요청 대상 없음 actor={}, emailHash={}", actorType, tokenService.hash(email));
			return new PasswordResetMessageResult(GENERIC_MESSAGE);
		}

		Optional<IssuedPasswordResetToken> issued = tokenService.issue(actorType, email);
		if (issued.isEmpty()) {
			return new PasswordResetMessageResult(GENERIC_MESSAGE);
		}

		IssuedPasswordResetToken token = issued.get();
		String resetUrl = resetUrl(actorType, email, token.rawToken());
		try {
			mailSender.send(actorType, email, resetUrl, properties.tokenTtlSeconds() / 60);
			log.info("비밀번호 재설정 링크 발송 actor={}, accountId={}", actorType, account.get().id());
		} catch (RuntimeException exception) {
			tokenService.discard(actorType, email, token.tokenHash());
			log.error("비밀번호 재설정 링크 발송 실패 actor={}, accountId={}", actorType, account.get().id(), exception);
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
		if (activeAccount(actorType, email).isEmpty() || !tokenService.isValid(actorType, email, command.token())) {
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

		AccountHandle account = activeAccount(actorType, email).orElseThrow(this::invalidToken);
		validateNewPassword(command.password(), account.encodedPassword());
		account.passwordUpdater().accept(passwordEncoder.encode(command.password()));
		tokenRepository.delete(token);
		authSessionService.revokeAll(actorType, account.id(), "PASSWORD_RESET");
		log.info("비밀번호 재설정 완료 actor={}, accountId={}", actorType, account.id());
		return new PasswordResetMessageResult("비밀번호가 변경되었습니다.");
	}

	private Optional<AccountHandle> activeAccount(AccountActorType actorType, String email) {
		return switch (actorType) {
			case STAFF -> staffRepository.findByEmailAndDeletedAtIsNull(email)
				.filter(AccountStaff::isActive)
				.map(account -> new AccountHandle(account.id(), account.password(), account::changePassword));
			case PARTNER -> partnerRepository.findByEmailAndDeletedAtIsNull(email)
				.filter(AccountPartner::isActive)
				.map(account -> new AccountHandle(account.id(), account.password(), account::changePassword));
			case BEAUTY -> beautyRepository.findByEmailAndDeletedAtIsNull(email)
				.filter(AccountBeauty::isActive)
				.map(account -> new AccountHandle(account.id(), account.password(), account::changePassword));
			case USER -> userRepository.findByEmailAndDeletedAtIsNull(email)
				.filter(AccountUser::isActive)
				.map(account -> new AccountHandle(account.id(), account.password(), account::changePassword));
		};
	}

	private String resetUrl(AccountActorType actorType, String email, String token) {
		URI uri = UriComponentsBuilder.fromUriString(properties.resetUrl(actorType))
			.queryParam("actor", actorType.name().toLowerCase(Locale.ROOT))
			.queryParam("email", email)
			.queryParam("token", token)
			.build()
			.encode()
			.toUri();
		return uri.toString();
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

	private record AccountHandle(Long id, String encodedPassword, Consumer<String> passwordUpdater) {
	}
}
