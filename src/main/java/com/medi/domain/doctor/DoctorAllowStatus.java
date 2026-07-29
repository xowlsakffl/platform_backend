package com.medi.domain.doctor;

public enum DoctorAllowStatus {
	PENDING("신청"),
	APPROVED("승인"),
	REJECTED("반려");

	private final String label;

	DoctorAllowStatus(String label) {
		this.label = label;
	}

	public String label() {
		return label;
	}
}
