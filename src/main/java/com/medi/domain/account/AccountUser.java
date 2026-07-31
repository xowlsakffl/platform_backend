package com.medi.domain.account;

import com.medi.domain.common.BaseTimeEntity;
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
@Table(name = "account_users")
public class AccountUser extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private String name;

	@Column(nullable = false, unique = true)
	private String nickname;

	@Column(nullable = false, unique = true)
	private String email;

	@Column(length = 50)
	private String phone;

	@Column(name = "signup_channel", nullable = false, length = 30)
	private String signupChannel = "EMAIL";

	@Column(name = "email_verified_at")
	private LocalDateTime emailVerifiedAt;

	@Column(nullable = false)
	private String password;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private AccountUserStatus status = AccountUserStatus.ACTIVE;

	@Column(name = "warning_count", nullable = false)
	private int warningCount;

	@Column(name = "blocked_at")
	private LocalDateTime blockedAt;

	@Column(name = "deleted_at")
	private LocalDateTime deletedAt;

	@Column(name = "last_login_at")
	private LocalDateTime lastLoginAt;

	protected AccountUser() {
	}

	public Long id() {
		return id;
	}

	public String name() {
		return name;
	}

	public String nickname() {
		return nickname;
	}

	public String email() {
		return email;
	}

	public String password() {
		return password;
	}

	public void changePassword(String encodedPassword) {
		this.password = Objects.requireNonNull(encodedPassword);
	}

	public boolean isActive() {
		return deletedAt == null && status == AccountUserStatus.ACTIVE;
	}

	public void markLoggedIn() {
		this.lastLoginAt = LocalDateTime.now();
	}
}
