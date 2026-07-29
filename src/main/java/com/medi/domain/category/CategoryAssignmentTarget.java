package com.medi.domain.category;

public enum CategoryAssignmentTarget {
	HOSPITAL(1),
	DOCTOR(1),
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
		return category.domain() == CategoryDomain.MEDICAL
			&& category.status() == CategoryStatus.ACTIVE
			&& category.depth() == selectableDepth;
	}
}
