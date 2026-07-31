package com.medi.application.auth;

import com.medi.application.auth.result.IssuedPasswordResetToken;
import com.medi.common.security.PasswordResetProperties;
import com.medi.domain.account.AccountActorType;
import com.medi.domain.auth.PasswordResetToken;
import com.medi.infrastructure.persistence.auth.PasswordResetTokenRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PasswordResetTokenService {

	private static final SecureRandom SECURE_RANDOM = new SecureRandom();

	private final PasswordResetTokenRepository tokenRepository;
	private final PasswordResetProperties properties;

	public PasswordResetTokenService(
		PasswordResetTokenRepository tokenRepository,
		PasswordResetProperties properties
	) {
		this.tokenRepository = tokenRepository;
		this.properties = properties;
	}

	@Transactional
	public Optional<IssuedPasswordResetToken> issue(AccountActorType actorType, String email) {
		LocalDateTime now = LocalDateTime.now();
		PasswordResetToken token = tokenRepository.findForUpdate(actorType, email).orElse(null);
		if (token != null && token.issuedAt().isAfter(now.minusSeconds(properties.tokenThrottleSeconds()))) {
			return Optional.empty();
		}

		String rawToken = newRawToken();
		String tokenHash = hash(rawToken);
		LocalDateTime expiresAt = now.plusSeconds(properties.tokenTtlSeconds());
		if (token == null) {
			tokenRepository.save(PasswordResetToken.create(actorType, email, tokenHash, now, expiresAt));
		} else {
			token.issue(tokenHash, now, expiresAt);
		}
		return Optional.of(new IssuedPasswordResetToken(rawToken, tokenHash));
	}

	@Transactional(readOnly = true)
	public boolean isValid(AccountActorType actorType, String email, String rawToken) {
		return tokenRepository.findByActorTypeAndEmail(actorType, email)
			.filter(token -> !token.isExpired(LocalDateTime.now()))
			.map(token -> constantTimeEquals(token.tokenHash(), hash(rawToken)))
			.orElse(false);
	}

	@Transactional
	public void discard(AccountActorType actorType, String email, String expectedHash) {
		tokenRepository.findForUpdate(actorType, email)
			.filter(token -> constantTimeEquals(token.tokenHash(), expectedHash))
			.ifPresent(tokenRepository::delete);
	}

	@Transactional
	public int cleanup() {
		return tokenRepository.deleteExpired(LocalDateTime.now());
	}

	public String hash(String value) {
		try {
			byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(digest);
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("SHA-256 알고리즘을 사용할 수 없습니다.", exception);
		}
	}

	public boolean constantTimeEquals(String expected, String actual) {
		if (expected == null || actual == null) {
			return false;
		}
		return MessageDigest.isEqual(
			expected.getBytes(StandardCharsets.UTF_8),
			actual.getBytes(StandardCharsets.UTF_8)
		);
	}

	private String newRawToken() {
		byte[] bytes = new byte[48];
		SECURE_RANDOM.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}
}
