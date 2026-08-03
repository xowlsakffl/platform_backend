package com.platform.application.partner.result;

import com.fasterxml.jackson.annotation.JsonProperty;

public record OperationHistoryChangeResult(
	@JsonProperty("field_key") String fieldKey,
	@JsonProperty("before_value") String beforeValue,
	@JsonProperty("after_value") String afterValue
) {
}
