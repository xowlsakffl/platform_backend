package com.platform.domain.account;

import com.platform.domain.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "staff_roles")
public class StaffRole extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 100)
	private String name;

	@Column(name = "display_name", nullable = false, length = 100)
	private String displayName;

	@ManyToMany(fetch = FetchType.LAZY)
	@JoinTable(
		name = "staff_role_permission_assignments",
		joinColumns = @JoinColumn(name = "staff_role_id"),
		inverseJoinColumns = @JoinColumn(name = "staff_permission_id")
	)
	private Set<StaffPermission> permissions = new LinkedHashSet<>();

	protected StaffRole() {
	}

	public String name() {
		return name;
	}

	public Set<StaffPermission> permissions() {
		return permissions;
	}
}
