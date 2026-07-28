package com.medi.domain.category;

import com.medi.domain.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
	private String domain;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "parent_id")
	private Category parent;

	@Column(nullable = false)
	private byte depth;

	@Column(nullable = false, length = 120)
	private String name;

	@Column(length = 80)
	private String code;

	@Column(name = "full_path")
	private String fullPath;

	@Column(name = "sort_order", nullable = false)
	private int sortOrder;

	@Column(nullable = false, length = 20)
	private String status = "ACTIVE";

	@Column(name = "is_menu_visible", nullable = false)
	private boolean menuVisible = true;

	protected Category() {
	}

	public Long id() {
		return id;
	}

	public String domain() {
		return domain;
	}

	public String name() {
		return name;
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
}
