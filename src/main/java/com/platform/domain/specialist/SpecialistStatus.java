package com.platform.domain.specialist;

public enum SpecialistStatus {
	VISIBLE("노출"),
	HIDDEN("미노출");

	private final String label;

	SpecialistStatus(String label) {
		this.label = label;
	}

	public String label() {
		return label;
	}
}
