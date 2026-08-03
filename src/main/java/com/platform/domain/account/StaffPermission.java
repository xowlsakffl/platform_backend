package com.platform.domain.account;

import com.platform.domain.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "staff_permissions")
public class StaffPermission extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 120)
	private String code;

	@Column(name = "display_name", nullable = false, length = 120)
	private String displayName;

	protected StaffPermission() {
	}

	public String code() {
		return code;
	}
}
