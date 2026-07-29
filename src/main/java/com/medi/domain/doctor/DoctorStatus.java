package com.medi.domain.doctor;

public enum DoctorStatus {
	VISIBLE("노출"),
	HIDDEN("미노출");

	private final String label;

	DoctorStatus(String label) {
		this.label = label;
	}

	public String label() {
		return label;
	}
}
