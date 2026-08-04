package com.platform.domain.category;

public enum CategoryAssignmentTarget {
	PARTNER(1, CategoryUsageType.PARTNER_CATEGORY),
	PARTNER_OPTION(2, CategoryUsageType.PARTNER_OPTION_CATEGORY);

	private final int selectableDepth;
	private final CategoryUsageType usage;

	CategoryAssignmentTarget(int selectableDepth, CategoryUsageType usage) {
		this.selectableDepth = selectableDepth;
		this.usage = usage;
	}

	public String code() {
		return name();
	}

	public int selectableDepth() {
		return selectableDepth;
	}

	public CategoryUsageType usage() {
		return usage;
	}

	public boolean accepts(Category category) {
		return category.domain() == CategoryDomain.PARTNER
			&& category.status() == CategoryStatus.ACTIVE
			&& category.depth() == selectableDepth;
	}
}
