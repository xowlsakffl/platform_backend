package com.platform.domain.specialist;

public enum SpecialistAllowStatus {
	PENDING("신청"),
	APPROVED("승인"),
	REJECTED("반려");

	private final String label;

	SpecialistAllowStatus(String label) {
		this.label = label;
	}

	public String label() {
		return label;
	}
}
