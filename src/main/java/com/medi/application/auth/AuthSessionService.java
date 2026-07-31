package com.medi.application.auth;

import com.medi.application.auth.command.AuthClientContext;
import com.medi.application.auth.result.RotatedAuthSessionResult;
import com.medi.common.error.ApiException;
import com.medi.common.error.ErrorCode;
import com.medi.common.error.RefreshTokenReuseException;
import com.medi.common.security.AuthSessionProperties;
import com.medi.domain.account.AccountActorType;
import com.medi.domain.auth.AuthSession;
import com.medi.infrastructure.persistence.auth.AuthSessionRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthSessionService {

	private static final SecureRandom SECURE_RANDOM = new SecureRandom();

	private final AuthSessionRepository sessionRepository;
	private final AuthSessionProperties properties;

	public AuthSessionService(AuthSessionRepository sessionRepository, AuthSessionProperties properties) {
		this.sessionRepository = sessionRepository;
		this.properties = properties;
	}

	@Transactional
	public RotatedAuthSessionResult create(
		AccountActorType actorType,
		Long accountId,
		boolean persistent,
		AuthClientContext client
	) {
		String sessionId = UUID.randomUUID().toString();
		String refreshToken = newRefreshToken(sessionId);
		long ttlSeconds = properties.ttlSeconds(actorType, persistent);
		LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(ttlSeconds);

		sessionRepository.save(AuthSession.create(
			sessionId,
			actorType,
			accountId,
			hash(refreshToken),
			persistent,
			expiresAt,
			client.ipAddress(),
			client.userAgent()
		));

		return new RotatedAuthSessionResult(
			sessionId,
			actorType,
			accountId,
			refreshToken,
			ttlSeconds,
			persistent
		);
	}

	@Transactional(noRollbackFor = RefreshTokenReuseException.class)
	public RotatedAuthSessionResult rotate(
		AccountActorType expectedActorType,
		String refreshToken,
		AuthClientContext client
	) {
		String sessionId = sessionId(refreshToken);
		AuthSession session = sessionRepository.findByIdForUpdate(sessionId).orElseThrow(this::invalidRefreshToken);
		LocalDateTime now = LocalDateTime.now();

		if (session.actorType() != expectedActorType || !session.isActive(now)) {
			throw invalidRefreshToken();
		}

		String presentedHash = hash(refreshToken);
		if (!constantTimeEquals(session.refreshTokenHash(), presentedHash)) {
			if (constantTimeEquals(session.previousRefreshTokenHash(), presentedHash)
				&& session.isPreviousTokenUsable(now)) {
				throw new ApiException(ErrorCode.TOKEN_ERROR, "이미 갱신된 인증 세션입니다. 다시 시도해주세요.");
			}
			session.revoke("REFRESH_TOKEN_REUSE");
			sessionRepository.saveAndFlush(session);
			throw new RefreshTokenReuseException();
		}

		String nextRefreshToken = newRefreshToken(session.id());
		session.rotate(
			hash(nextRefreshToken),
			now.plusSeconds(properties.rotationGraceSeconds()),
			client.ipAddress(),
			client.userAgent()
		);

		long remainingSeconds = Math.max(1, ChronoUnit.SECONDS.between(now, session.expiresAt()));
		return new RotatedAuthSessionResult(
			session.id(),
			session.actorType(),
			session.accountId(),
			nextRefreshToken,
			remainingSeconds,
			session.persistent()
		);
	}

	@Transactional(readOnly = true)
	public boolean isActive(String sessionId, AccountActorType actorType, Long accountId) {
		if (sessionId == null || sessionId.isBlank()) {
			return false;
		}
		return sessionRepository.existsByIdAndActorTypeAndAccountIdAndRevokedAtIsNullAndExpiresAtAfter(
			sessionId,
			actorType,
			accountId,
			LocalDateTime.now()
		);
	}

	@Transactional
	public void revoke(String sessionId, AccountActorType actorType, Long accountId, String reason) {
		if (sessionId == null || sessionId.isBlank()) {
			return;
		}
		sessionRepository.findByIdForUpdate(sessionId)
			.filter(session -> session.actorType() == actorType && session.accountId().equals(accountId))
			.ifPresent(session -> session.revoke(reason));
	}

	@Transactional
	public void revokeAll(AccountActorType actorType, Long accountId, String reason) {
		sessionRepository.revokeAll(actorType, accountId, LocalDateTime.now(), reason);
	}

	@Transactional
	public int cleanup() {
		LocalDateTime now = LocalDateTime.now();
		return sessionRepository.deleteExpiredAndOldRevoked(now, now.minusDays(30));
	}

	private String newRefreshToken(String sessionId) {
		byte[] bytes = new byte[properties.refreshTokenBytes()];
		SECURE_RANDOM.nextBytes(bytes);
		return sessionId + "." + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	private String sessionId(String refreshToken) {
		if (refreshToken == null || refreshToken.isBlank()) {
			throw invalidRefreshToken();
		}
		int separator = refreshToken.indexOf('.');
		if (separator <= 0 || separator == refreshToken.length() - 1) {
			throw invalidRefreshToken();
		}
		String sessionId = refreshToken.substring(0, separator);
		try {
			UUID.fromString(sessionId);
			return sessionId;
		} catch (IllegalArgumentException exception) {
			throw invalidRefreshToken();
		}
	}

	private String hash(String value) {
		try {
			byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(digest);
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("SHA-256 알고리즘을 사용할 수 없습니다.", exception);
		}
	}

	private boolean constantTimeEquals(String expected, String actual) {
		if (expected == null || actual == null) {
			return false;
		}
		return MessageDigest.isEqual(
			expected.getBytes(StandardCharsets.UTF_8),
			actual.getBytes(StandardCharsets.UTF_8)
		);
	}

	private ApiException invalidRefreshToken() {
		return new ApiException(ErrorCode.TOKEN_ERROR, "리프레시 토큰이 유효하지 않습니다.");
	}
}
