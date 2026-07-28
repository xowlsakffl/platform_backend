package com.medi.application.hospital.result;

import java.time.LocalDateTime;

public record HospitalDeletedResult(
	Long deletedId,
	LocalDateTime deletedAt
) {
}
