package com.medi.domain.hospital;

public enum HospitalAllowStatus {
	PENDING("신청"),
	APPROVED("승인"),
	REJECTED("반려");

	private final String label;

	HospitalAllowStatus(String label) {
		this.label = label;
	}

	public String label() {
		return label;
	}
}
