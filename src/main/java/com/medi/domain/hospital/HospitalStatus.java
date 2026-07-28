package com.medi.domain.hospital;

public enum HospitalStatus {
	ACTIVE("정상"),
	SUSPENDED("운영중지"),
	WITHDRAWN("탈퇴");

	private final String label;

	HospitalStatus(String label) {
		this.label = label;
	}

	public String label() {
		return label;
	}
}
