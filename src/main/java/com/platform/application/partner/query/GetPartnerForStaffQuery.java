package com.platform.application.partner.query;

import java.util.Set;

public record GetPartnerForStaffQuery(Set<String> include) {

	public boolean includes(String value) {
		return include.contains(value);
	}
}
