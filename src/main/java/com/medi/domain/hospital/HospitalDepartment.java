package com.medi.domain.hospital;

public enum HospitalDepartment {
	PLASTIC_SURGERY("성형외과"),
	DERMATOLOGY("피부과"),
	CLINIC("의원"),
	DENTISTRY("치과"),
	OPHTHALMOLOGY("안과"),
	KOREAN_MEDICINE("한의원"),
	OTHER("기타");

	private final String label;

	HospitalDepartment(String label) {
		this.label = label;
	}

	public String label() {
		return label;
	}
}
