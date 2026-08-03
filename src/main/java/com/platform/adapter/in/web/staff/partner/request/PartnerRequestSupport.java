package com.platform.adapter.in.web.staff.partner.request;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class PartnerRequestSupport {

	public static final String PHONE_PATTERN = "^(?:|[0-9+\\-().\\s]{6,50})$";

	private PartnerRequestSupport() {
	}

	public static <T> List<T> list(List<T> value) {
		return value == null ? List.of() : value;
	}

	public static Set<Long> ids(Set<Long> value) {
		if (value == null) {
			return Set.of();
		}
		return new LinkedHashSet<>(value.stream()
			.filter(id -> id != null && id > 0)
			.toList());
	}
}
