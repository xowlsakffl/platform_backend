package com.platform.domain.category;

public enum CategoryAssignmentTarget {
	PARTNER(1),
	SPECIALIST(1),
	EVENT(3),
	REVIEW(3);

	private final int selectableDepth;

	CategoryAssignmentTarget(int selectableDepth) {
		this.selectableDepth = selectableDepth;
	}

	public String code() {
		return name();
	}

	public int selectableDepth() {
		return selectableDepth;
	}

	public boolean accepts(Category category) {
		return category.domain() == CategoryDomain.PARTNER
			&& category.status() == CategoryStatus.ACTIVE
			&& category.depth() == selectableDepth;
	}
}
