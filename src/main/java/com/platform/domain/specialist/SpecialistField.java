package com.platform.domain.specialist;

public enum SpecialistField {
	HAIR_DESIGNER(1, "헤어 디자이너"),
	NAIL_ARTIST(2, "네일아티스트"),
	ESTHETICIAN(3, "에스테티션"),
	WAXING_SPECIALIST(4, "왁싱 스페셜리스트"),
	TATTOO_ARTIST(5, "타투이스트"),
	SEMI_PERMANENT_ARTIST(6, "반영구 아티스트"),
	MASSAGE_THERAPIST(7, "마사지 테라피스트"),
	MAKEUP_ARTIST(8, "메이크업 아티스트"),
	OTHER(90, "기타");

	private final int code;
	private final String label;

	SpecialistField(int code, String label) {
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
