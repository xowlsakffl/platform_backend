package com.platform.domain.partner;

public enum PartnerContactType {
	REPRESENTATIVE_PHONE(1),
	REPRESENTATIVE_EMAIL(1),
	SMS_SENDER_PHONE(1),
	CALL_RECEIVER_PHONE(1),
	CONSULTATION_RECEIVER_PHONE(3),
	EVENT_NOTICE_RECEIVER_PHONE(3),
	NOTICE_MARKETING_EMAIL(3);

	private final int maxCount;

	PartnerContactType(int maxCount) {
		this.maxCount = maxCount;
	}

	public int maxCount() {
		return maxCount;
	}
}
