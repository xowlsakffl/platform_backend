package com.platform.domain.auth;

import com.platform.domain.account.AccountActorType;
import com.platform.domain.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "password_reset_tokens")
public class PasswordResetToken extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Enumerated(EnumType.STRING)
	@Column(name = "actor_type", nullable = false, length = 20)
	private AccountActorType actorType;

	@Column(nullable = false)
	private String email;

	@Column(name = "token_hash", nullable = false, length = 64)
	private String tokenHash;

	@Column(name = "issued_at", nullable = false)
	private LocalDateTime issuedAt;

	@Column(name = "expires_at", nullable = false)
	private LocalDateTime expiresAt;

	protected PasswordResetToken() {
	}

	public static PasswordResetToken create(
		AccountActorType actorType,
		String email,
		String tokenHash,
		LocalDateTime issuedAt,
		LocalDateTime expiresAt
	) {
		PasswordResetToken token = new PasswordResetToken();
		token.actorType = Objects.requireNonNull(actorType);
		token.email = Objects.requireNonNull(email);
		token.issue(tokenHash, issuedAt, expiresAt);
		return token;
	}

	public String tokenHash() {
		return tokenHash;
	}

	public LocalDateTime issuedAt() {
		return issuedAt;
	}

	public boolean isExpired(LocalDateTime now) {
		return !expiresAt.isAfter(now);
	}

	public void issue(String tokenHash, LocalDateTime issuedAt, LocalDateTime expiresAt) {
		this.tokenHash = Objects.requireNonNull(tokenHash);
		this.issuedAt = Objects.requireNonNull(issuedAt);
		this.expiresAt = Objects.requireNonNull(expiresAt);
	}
}
