package com.medi.application.hospital.result;

import java.time.LocalDateTime;

public record HospitalAccountResult(
	Long id,
	String nickname,
	String email,
	String phone,
	String status,
	LocalDateTime lastLoginAt
) {
}
