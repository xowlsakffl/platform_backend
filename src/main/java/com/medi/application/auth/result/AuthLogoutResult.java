package com.medi.application.auth.result;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AuthLogoutResult(
	@JsonProperty("logged_out")
	boolean loggedOut
) {
}
