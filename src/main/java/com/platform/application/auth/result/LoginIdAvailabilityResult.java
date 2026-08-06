package com.platform.application.auth.result;

import com.fasterxml.jackson.annotation.JsonProperty;

public record LoginIdAvailabilityResult(
	@JsonProperty("login_id") String loginId,
	boolean available
) {
}
