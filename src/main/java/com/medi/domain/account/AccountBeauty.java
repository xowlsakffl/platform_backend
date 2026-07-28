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

@Entity
@Table(name = "account_beauties")
public class AccountBeauty extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "beauty_id", unique = true)
	private Long beautyId;

	@Column(nullable = false)
	private String name;

	@Column(nullable = false, unique = true)
	private String nickname;

	@Column(nullable = false, unique = true)
	private String email;

	@Column(length = 50)
	private String phone;

	@Column(name = "email_verified_at")
	private LocalDateTime emailVerifiedAt;

	@Column(nullable = false)
	private String password;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private AccountBeautyStatus status = AccountBeautyStatus.SUSPENDED;

	@Column(name = "last_login_at")
	private LocalDateTime lastLoginAt;

	@Column(name = "deleted_at")
	private LocalDateTime deletedAt;

	protected AccountBeauty() {
	}

	public Long id() {
		return id;
	}

	public Long beautyId() {
		return beautyId;
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

	public boolean isActive() {
		return deletedAt == null && status == AccountBeautyStatus.ACTIVE;
	}

	public void markLoggedIn() {
		this.lastLoginAt = LocalDateTime.now();
	}
}
