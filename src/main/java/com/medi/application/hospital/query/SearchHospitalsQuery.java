package com.medi.application.hospital.query;

import com.medi.domain.hospital.HospitalAllowStatus;
import com.medi.domain.account.AccountHospitalStatus;
import com.medi.domain.hospital.HospitalStatus;
import java.util.List;

public record SearchHospitalsQuery(
	String q,
	List<HospitalStatus> status,
	List<AccountHospitalStatus> accountStatus,
	List<HospitalAllowStatus> allowStatus,
	List<Long> categoryIds,
	List<String> include,
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
