package com.platform.domain.account;

import com.platform.domain.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@Table(name = "account_staffs")
public class AccountStaff extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "login_id", nullable = false, unique = true, length = 30)
	private String loginId;

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

	private String department;

	@Column(name = "job_title")
	private String jobTitle;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private AccountStaffStatus status = AccountStaffStatus.ACTIVE;

	@Column(name = "last_login_at")
	private LocalDateTime lastLoginAt;

	@Column(name = "deleted_at")
	private LocalDateTime deletedAt;

	@ManyToMany(fetch = FetchType.LAZY)
	@JoinTable(
		name = "account_staff_roles",
		joinColumns = @JoinColumn(name = "account_staff_id"),
		inverseJoinColumns = @JoinColumn(name = "staff_role_id")
	)
	private Set<StaffRole> roles = new LinkedHashSet<>();

	protected AccountStaff() {
	}

	public static AccountStaff create(
		String loginId,
		String name,
		String nickname,
		String email,
		String encodedPassword
	) {
		AccountStaff staff = new AccountStaff();
		staff.loginId = Objects.requireNonNull(loginId);
		staff.name = Objects.requireNonNull(name);
		staff.nickname = Objects.requireNonNull(nickname);
		staff.email = Objects.requireNonNull(email);
		staff.password = Objects.requireNonNull(encodedPassword);
		staff.emailVerifiedAt = LocalDateTime.now();
		staff.status = AccountStaffStatus.ACTIVE;
		return staff;
	}

	public Long id() {
		return id;
	}

	public String loginId() {
		return loginId;
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

	public void changePassword(String encodedPassword) {
		this.password = Objects.requireNonNull(encodedPassword);
	}

	public AccountStaffStatus status() {
		return status;
	}

	public boolean isActive() {
		return deletedAt == null && status == AccountStaffStatus.ACTIVE;
	}

	public Set<String> permissionCodes() {
		return roles.stream()
			.flatMap(role -> role.permissions().stream())
			.map(StaffPermission::code)
			.collect(Collectors.toCollection(LinkedHashSet::new));
	}

	public void assignRole(StaffRole role) {
		roles.add(Objects.requireNonNull(role));
	}

	public void markLoggedIn() {
		this.lastLoginAt = LocalDateTime.now();
	}
}
