package com.platform.application.account.result;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PartnerAccountManagementHistoryChangeForStaffResult(
	@JsonProperty("field_key") String fieldKey,
	@JsonProperty("before_value") String beforeValue,
	@JsonProperty("after_value") String afterValue
) {
}
