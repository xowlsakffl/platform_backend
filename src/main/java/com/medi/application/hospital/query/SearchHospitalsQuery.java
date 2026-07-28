package com.medi.application.hospital.query;

import com.medi.domain.hospital.HospitalAllowStatus;
import com.medi.domain.hospital.HospitalDepartment;
import com.medi.domain.hospital.HospitalStatus;
import java.util.List;

public record SearchHospitalsQuery(
	String q,
	List<HospitalStatus> status,
	List<HospitalAllowStatus> allowStatus,
	List<HospitalDepartment> department,
	List<Long> categoryIds,
	Boolean dormant,
	String startDate,
	String endDate,
	String updatedStartDate,
	String updatedEndDate,
	String sort,
	String direction,
	int page,
	int perPage
) {
}
