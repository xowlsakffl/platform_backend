package com.medi.domain.hospital;

public enum HospitalInterpretationLanguage {
	JAPANESE("일본어"),
	ENGLISH("영어"),
	THAI("태국어"),
	CHINESE("중국어"),
	TAIWANESE_CHINESE("대만 중국어");

	private final String label;

	HospitalInterpretationLanguage(String label) {
		this.label = label;
	}

	public String label() {
		return label;
	}
}
