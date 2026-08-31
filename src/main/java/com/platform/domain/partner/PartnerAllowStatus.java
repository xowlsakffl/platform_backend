package com.platform.domain.partner;

public enum PartnerAllowStatus {
	DRAFT("Draft"),
	REVIEW_REQUESTED("검수 신청"),
	IN_REVIEW("검수 중"),
	APPROVED("승인"),
	REJECTED("반려");

	private final String label;

	PartnerAllowStatus(String label) {
		this.label = label;
	}

	public String label() {
		return label;
	}

	public boolean canTransitionTo(PartnerAllowStatus next) {
		if (this == next) {
			return true;
		}
		if (this == REVIEW_REQUESTED) {
			return next == IN_REVIEW;
		}
		return this == IN_REVIEW && (next == APPROVED || next == REJECTED);
	}
}
