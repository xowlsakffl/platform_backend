package com.platform.domain.specialist;

public enum SpecialistScheduleMode {
	INHERIT_PARTNER_HOURS("업체 운영시간 사용"),
	CUSTOM_HOURS("개별 운영시간 사용");

	private final String label;

	SpecialistScheduleMode(String label) {
		this.label = label;
	}

	public String label() {
		return label;
	}
}
