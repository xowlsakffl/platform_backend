package com.medi.application.hospital.query;

import java.util.Set;

public record GetHospitalForStaffQuery(Set<String> include) {

	public boolean includes(String value) {
		return include.contains(value);
	}
}
