package com.platform.domain.partner;

public enum PartnerAllowStatus {
	DRAFT("Draft"),
	PENDING("신청"),
	APPROVED("승인"),
	REJECTED("반려");

	private final String label;

	PartnerAllowStatus(String label) {
		this.label = label;
	}

	public String label() {
		return label;
	}
}
