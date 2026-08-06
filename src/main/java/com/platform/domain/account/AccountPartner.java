package com.platform.domain.account;

import com.platform.domain.common.BaseTimeEntity;
import com.platform.domain.partner.Partner;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "account_partners")
public class AccountPartner extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "partner_id", nullable = false)
	private Partner partner;

	@Column(name = "login_id", nullable = false, unique = true, length = 30)
	private String loginId;

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
	private AccountPartnerStatus status = AccountPartnerStatus.ACTIVE;

	@Column(name = "last_login_at")
	private LocalDateTime lastLoginAt;

	@Column(name = "deleted_at")
	private LocalDateTime deletedAt;

	protected AccountPartner() {
	}

	public static AccountPartner create(
		Partner partner,
		String loginId,
		String email,
		String phone,
		String encodedPassword,
		AccountPartnerStatus status
	) {
		AccountPartner account = new AccountPartner();
		account.partner = Objects.requireNonNull(partner);
		account.loginId = Objects.requireNonNull(loginId);
		account.email = Objects.requireNonNull(email);
		account.phone = phone;
		account.emailVerifiedAt = LocalDateTime.now();
		account.password = Objects.requireNonNull(encodedPassword);
		account.status = status == null ? AccountPartnerStatus.ACTIVE : status;
		return account;
	}

	public Long id() {
		return id;
	}

	public Long partnerId() {
		return partner == null ? null : partner.id();
	}

	public String loginId() {
		return loginId;
	}

	public String partnerName() {
		return partner == null ? null : partner.name();
	}

	public String email() {
		return email;
	}

	public String phone() {
		return phone;
	}

	public String password() {
		return password;
	}

	public void changePassword(String encodedPassword) {
		this.password = Objects.requireNonNull(encodedPassword);
	}

	public AccountPartnerStatus status() {
		return status;
	}

	public void changeStatus(AccountPartnerStatus status) {
		this.status = Objects.requireNonNull(status);
	}

	public LocalDateTime lastLoginAt() {
		return lastLoginAt;
	}

	public boolean isActive() {
		return deletedAt == null
			&& status == AccountPartnerStatus.ACTIVE
			&& partner != null
			&& partner.deletedAt() == null
			&& partner.status() != com.platform.domain.partner.PartnerStatus.WITHDRAWN;
	}

	public void markLoggedIn() {
		this.lastLoginAt = LocalDateTime.now();
	}
}
