package com.platform.domain.partner;

public enum PartnerIndustry {
	SEMI_PERMANENT("반영구"),
	ESTHETIC("에스테틱"),
	HAIR_SALON("미용실"),
	WAXING("왁싱"),
	TATTOO("타투"),
	NAIL_SHOP("네일아트"),
	MASSAGE("마사지"),
	OTHER("기타");

	private final String label;

	PartnerIndustry(String label) {
		this.label = label;
	}

	public String label() {
		return label;
	}
}
