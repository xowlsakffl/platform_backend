package com.medi.domain.category;

import com.medi.domain.common.BaseTimeEntity;
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
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
	name = "category_usages",
	uniqueConstraints = @UniqueConstraint(
		name = "category_usages_usage_category_unique",
		columnNames = {"usage", "category_id"}
	)
)
public class CategoryUsage extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "usage", nullable = false, length = 60)
	@Enumerated(EnumType.STRING)
	private CategoryUsageType usage;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "category_id", nullable = false)
	private Category category;

	@Column(name = "sort_order", nullable = false)
	private int sortOrder;

	@Column(nullable = false, length = 20)
	@Enumerated(EnumType.STRING)
	private CategoryStatus status = CategoryStatus.ACTIVE;

	protected CategoryUsage() {
	}

	public CategoryUsageType usage() {
		return usage;
	}

	public Category category() {
		return category;
	}

	public int sortOrder() {
		return sortOrder;
	}
}
