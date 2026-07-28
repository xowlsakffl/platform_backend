package com.medi.domain.hospital;

public enum HospitalContactType {
	REPRESENTATIVE_PHONE(1),
	SMS_SENDER_PHONE(1),
	CALL_RECEIVER_PHONE(1),
	CONSULTATION_RECEIVER_PHONE(3),
	EVENT_NOTICE_RECEIVER_PHONE(3),
	NOTICE_MARKETING_EMAIL(3);

	private final int maxCount;

	HospitalContactType(int maxCount) {
		this.maxCount = maxCount;
	}

	public int maxCount() {
		return maxCount;
	}
}
