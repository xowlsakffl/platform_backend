package com.medi.application.hospital.result;

public record OperationHistoryChangeResult(
	String fieldKey,
	String beforeValue,
	String afterValue
) {
}
