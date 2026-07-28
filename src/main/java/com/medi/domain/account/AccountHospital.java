package com.medi.domain.account;

import com.medi.domain.common.BaseTimeEntity;
import com.medi.domain.hospital.Hospital;
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

@Entity
@Table(name = "account_hospitals")
public class AccountHospital extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "hospital_id", nullable = false)
	private Hospital hospital;

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
	@Column(nullable = false)
	private AccountHospitalStatus status = AccountHospitalStatus.SUSPENDED;

	@Column(name = "last_login_at")
	private LocalDateTime lastLoginAt;

	@Column(name = "deleted_at")
	private LocalDateTime deletedAt;

	protected AccountHospital() {
	}

	public Long id() {
		return id;
	}

	public Long hospitalId() {
		return hospital == null ? null : hospital.id();
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

	public String phone() {
		return phone;
	}

	public String password() {
		return password;
	}

	public AccountHospitalStatus status() {
		return status;
	}

	public LocalDateTime lastLoginAt() {
		return lastLoginAt;
	}

	public boolean isActive() {
		return deletedAt == null && status == AccountHospitalStatus.ACTIVE;
	}

	public void markLoggedIn() {
		this.lastLoginAt = LocalDateTime.now();
	}
}
