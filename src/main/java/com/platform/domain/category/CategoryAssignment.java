package com.platform.domain.category;

import com.platform.domain.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
	name = "category_assignments",
	uniqueConstraints = @UniqueConstraint(
		name = "category_assignments_target_category_unique",
		columnNames = {"categorizable_type", "categorizable_id", "category_id"}
	)
)
public class CategoryAssignment extends BaseTimeEntity {

	public static final String PARTNER_TARGET_TYPE = CategoryAssignmentTarget.PARTNER.code();
	public static final String PARTNER_OPTION_TARGET_TYPE = CategoryAssignmentTarget.PARTNER_OPTION.code();

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "categorizable_type", nullable = false, length = 191)
	private String categorizableType;

	@Column(name = "categorizable_id", nullable = false)
	private Long categorizableId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "category_id", nullable = false)
	private Category category;

	@Column(name = "is_primary", nullable = false)
	private boolean primary;

	protected CategoryAssignment() {
	}

	public CategoryAssignment(String categorizableType, Long categorizableId, Category category, boolean primary) {
		this.categorizableType = categorizableType;
		this.categorizableId = categorizableId;
		this.category = category;
		this.primary = primary;
	}

	public Category category() {
		return category;
	}

	public Long categorizableId() {
		return categorizableId;
	}

	public boolean primary() {
		return primary;
	}

	public void changePrimary(boolean primary) {
		this.primary = primary;
	}
}
