package com.medi.domain.category;

public enum CategoryGroup {
	SURGERY("성형"),
	TREATMENT("시술");

	private final String label;

	CategoryGroup(String label) {
		this.label = label;
	}

	public String label() {
		return label;
	}
}
