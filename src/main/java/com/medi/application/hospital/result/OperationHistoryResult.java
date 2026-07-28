package com.medi.application.hospital.result;

import java.time.LocalDateTime;
import java.util.List;

public record OperationHistoryResult(
	Long id,
	String action,
	String reason,
	LocalDateTime createdAt,
	List<OperationHistoryChangeResult> changes
) {
}
