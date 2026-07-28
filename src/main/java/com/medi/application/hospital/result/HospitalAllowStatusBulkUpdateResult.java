package com.medi.application.hospital.result;

import java.util.List;

public record HospitalAllowStatusBulkUpdateResult(
	int updatedCount,
	String allowStatus,
	List<Long> ids
) {
}
