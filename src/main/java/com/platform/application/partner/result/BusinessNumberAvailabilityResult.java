package com.platform.application.partner.result;

import com.fasterxml.jackson.annotation.JsonProperty;

public record BusinessNumberAvailabilityResult(
	@JsonProperty("already_registered") boolean alreadyRegistered
) {
}
