package com.platform.domain.auth;

import com.platform.domain.account.AccountActorType;
import com.platform.domain.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "auth_sessions")
public class AuthSession extends BaseTimeEntity {

	@Id
	@Column(length = 36)
	private String id;

	@Enumerated(EnumType.STRING)
	@Column(name = "actor_type", nullable = false, length = 20)
	private AccountActorType actorType;

	@Column(name = "account_id", nullable = false)
	private Long accountId;

	@Column(name = "refresh_token_hash", nullable = false, length = 64)
	private String refreshTokenHash;

	@Column(name = "previous_refresh_token_hash", length = 64)
	private String previousRefreshTokenHash;

	@Column(name = "previous_token_valid_until")
	private LocalDateTime previousTokenValidUntil;

	@Column(nullable = false)
	private boolean persistent;

	@Column(name = "expires_at", nullable = false)
	private LocalDateTime expiresAt;

	@Column(name = "last_used_at", nullable = false)
	private LocalDateTime lastUsedAt;

	@Column(name = "revoked_at")
	private LocalDateTime revokedAt;

	@Column(name = "revocation_reason", length = 80)
	private String revocationReason;

	@Column(name = "ip_address", length = 45)
	private String ipAddress;

	@Column(name = "user_agent", length = 500)
	private String userAgent;

	protected AuthSession() {
	}

	public static AuthSession create(
		String id,
		AccountActorType actorType,
		Long accountId,
		String refreshTokenHash,
		boolean persistent,
		LocalDateTime expiresAt,
		String ipAddress,
		String userAgent
	) {
		AuthSession session = new AuthSession();
		session.id = Objects.requireNonNull(id);
		session.actorType = Objects.requireNonNull(actorType);
		session.accountId = Objects.requireNonNull(accountId);
		session.refreshTokenHash = Objects.requireNonNull(refreshTokenHash);
		session.persistent = persistent;
		session.expiresAt = Objects.requireNonNull(expiresAt);
		session.lastUsedAt = LocalDateTime.now();
		session.ipAddress = ipAddress;
		session.userAgent = userAgent;
		return session;
	}

	public String id() {
		return id;
	}

	public AccountActorType actorType() {
		return actorType;
	}

	public Long accountId() {
		return accountId;
	}

	public String refreshTokenHash() {
		return refreshTokenHash;
	}

	public String previousRefreshTokenHash() {
		return previousRefreshTokenHash;
	}

	public boolean isPreviousTokenUsable(LocalDateTime now) {
		return previousTokenValidUntil != null && previousTokenValidUntil.isAfter(now);
	}

	public boolean persistent() {
		return persistent;
	}

	public LocalDateTime expiresAt() {
		return expiresAt;
	}

	public boolean isActive(LocalDateTime now) {
		return revokedAt == null && expiresAt.isAfter(now);
	}

	public void rotate(
		String nextRefreshTokenHash,
		LocalDateTime previousTokenValidUntil,
		String ipAddress,
		String userAgent
	) {
		this.previousRefreshTokenHash = this.refreshTokenHash;
		this.previousTokenValidUntil = previousTokenValidUntil;
		this.refreshTokenHash = Objects.requireNonNull(nextRefreshTokenHash);
		this.lastUsedAt = LocalDateTime.now();
		this.ipAddress = ipAddress;
		this.userAgent = userAgent;
	}

	public void revoke(String reason) {
		if (revokedAt != null) {
			return;
		}
		this.revokedAt = LocalDateTime.now();
		this.revocationReason = reason;
	}
}
