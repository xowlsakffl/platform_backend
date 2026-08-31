package com.platform.domain.partner;

public enum PartnerStatus {
	ACTIVE("정상"),
	SUSPENDED("운영중지"),
	WITHDRAWN("탈퇴");

	private final String label;

	PartnerStatus(String label) {
		this.label = label;
	}

	public String label() {
		return label;
	}

	public boolean staffSelectable() {
		return this != WITHDRAWN;
	}
}
