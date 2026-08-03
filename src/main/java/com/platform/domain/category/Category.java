package com.platform.domain.category;

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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "categories")
public class Category extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 40)
	@Enumerated(EnumType.STRING)
	private CategoryDomain domain;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "parent_id")
	private Category parent;

	@Column(nullable = false)
	private byte depth;

	@Column(name = "group_code", length = 30)
	@Enumerated(EnumType.STRING)
	private CategoryGroup groupCode;

	@Column(nullable = false, length = 120)
	private String name;

	@Column(length = 80)
	private String code;

	@Column(name = "full_path")
	private String fullPath;

	@Column(name = "sort_order", nullable = false)
	private int sortOrder;

	@Column(nullable = false, length = 20)
	@Enumerated(EnumType.STRING)
	private CategoryStatus status = CategoryStatus.ACTIVE;

	@Column(name = "is_menu_visible", nullable = false)
	private boolean menuVisible = true;

	protected Category() {
	}

	public Category(
		CategoryDomain domain,
		Category parent,
		byte depth,
		CategoryGroup groupCode,
		String name,
		String code,
		String fullPath,
		int sortOrder,
		CategoryStatus status,
		boolean menuVisible
	) {
		this.domain = domain;
		this.parent = parent;
		this.depth = depth;
		this.groupCode = groupCode;
		this.name = name;
		this.code = code;
		this.fullPath = fullPath;
		this.sortOrder = sortOrder;
		this.status = status;
		this.menuVisible = menuVisible;
	}

	public void update(
		String name,
		String code,
		String fullPath,
		CategoryGroup groupCode,
		int sortOrder,
		CategoryStatus status,
		boolean menuVisible
	) {
		this.name = name;
		this.code = code;
		this.fullPath = fullPath;
		this.groupCode = groupCode;
		this.sortOrder = sortOrder;
		this.status = status;
		this.menuVisible = menuVisible;
	}

	public void updateInheritedValues(String fullPath, CategoryGroup groupCode) {
		this.fullPath = fullPath;
		this.groupCode = groupCode;
	}

	public Long id() {
		return id;
	}

	public CategoryDomain domain() {
		return domain;
	}

	public Category parent() {
		return parent;
	}

	public Long parentId() {
		return parent == null ? null : parent.id();
	}

	public String name() {
		return name;
	}

	public String code() {
		return code;
	}

	public CategoryGroup groupCode() {
		return groupCode;
	}

	public String fullPath() {
		return fullPath;
	}

	public int depth() {
		return depth;
	}

	public int sortOrder() {
		return sortOrder;
	}

	public CategoryStatus status() {
		return status;
	}

	public boolean menuVisible() {
		return menuVisible;
	}
}
