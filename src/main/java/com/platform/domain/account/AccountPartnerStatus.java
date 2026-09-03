package com.platform.domain.account;

public enum AccountPartnerStatus {
	ACTIVE,
	BLOCKED,
	WITHDRAWN;

	public boolean staffSelectable() {
		return this != WITHDRAWN;
	}
}
