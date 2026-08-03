package com.platform.application.auth.result;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AuthTokenResult(
	@JsonProperty("token_type")
	String tokenType,
	@JsonProperty("access_token")
	String accessToken,
	@JsonProperty("expires_in")
	long expiresIn,
	AuthActorResult actor
) {
}
