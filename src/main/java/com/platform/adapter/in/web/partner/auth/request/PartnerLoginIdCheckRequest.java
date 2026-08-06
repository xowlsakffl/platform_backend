package com.platform.adapter.in.web.partner.auth.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PartnerLoginIdCheckRequest(
	@JsonProperty("login_id") @NotBlank @Size(min = 4, max = 30)
	@Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9._-]{3,29}$") String loginId
) {
}
