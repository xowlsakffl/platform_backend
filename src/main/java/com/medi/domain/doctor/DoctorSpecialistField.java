package com.medi.domain.doctor;

public enum DoctorSpecialistField {
	PLASTIC_SURGERY(1, "성형외과"),
	SURGERY(2, "외과"),
	OTOLARYNGOLOGY(3, "이비인후과"),
	FAMILY_MEDICINE(4, "가정의학과"),
	OBSTETRICS_GYNECOLOGY(5, "산부인과"),
	ORAL_MAXILLOFACIAL_SURGERY(6, "구강악안면외과"),
	ANESTHESIOLOGY_PAIN_MEDICINE(7, "마취통증의학과"),
	KOREAN_MEDICINE(8, "한의학과"),
	DENTISTRY(9, "치과"),
	ORTHODONTICS(10, "치과교정과"),
	DERMATOLOGY(11, "피부과"),
	OPHTHALMOLOGY(12, "안과"),
	INTERNAL_MEDICINE(13, "내과"),
	NEUROLOGY(14, "신경과"),
	ORTHOPEDICS(15, "정형외과"),
	NEUROSURGERY(16, "신경외과"),
	THORACIC_SURGERY(17, "흉부외과"),
	PEDIATRICS(19, "소아청소년과"),
	UROLOGY(20, "비뇨의학과"),
	RADIOLOGY(21, "영상의학과"),
	EMERGENCY_MEDICINE(22, "응급의학과"),
	REHABILITATION_MEDICINE(23, "재활의학과"),
	PROSTHODONTICS(24, "치과보철과"),
	PERIODONTICS(25, "치주과"),
	INTEGRATED_DENTISTRY(26, "통합치의학과"),
	PATHOLOGY(27, "병리과"),
	OCCUPATIONAL_ENVIRONMENTAL_MEDICINE(28, "직업환경의학과"),
	CONSERVATIVE_DENTISTRY(29, "치과보존과"),
	OTHER(90, "기타");

	private final int code;
	private final String label;

	DoctorSpecialistField(int code, String label) {
		this.code = code;
		this.label = label;
	}

	public int code() {
		return code;
	}

	public String label() {
		return label;
	}

}
