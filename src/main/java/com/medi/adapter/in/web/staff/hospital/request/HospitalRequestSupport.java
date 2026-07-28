package com.medi.adapter.in.web.staff.hospital.request;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

final class HospitalRequestSupport {

	static final String PHONE_PATTERN = "^[0-9+\\-().\\s]{6,50}$";

	private HospitalRequestSupport() {
	}

	static <T> List<T> list(List<T> value) {
		return value == null ? List.of() : value;
	}

	static Set<Long> ids(Set<Long> value) {
		if (value == null) {
			return Set.of();
		}
		return new LinkedHashSet<>(value.stream()
			.filter(id -> id != null && id > 0)
			.toList());
	}
}
