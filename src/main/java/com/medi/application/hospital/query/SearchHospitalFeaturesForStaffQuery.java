package com.medi.application.hospital.query;

import com.medi.domain.hospital.HospitalFeatureStatus;
import java.util.List;

public record SearchHospitalFeaturesForStaffQuery(
	String q,
	List<HospitalFeatureStatus> status,
	String sort,
	String direction
) {
}
