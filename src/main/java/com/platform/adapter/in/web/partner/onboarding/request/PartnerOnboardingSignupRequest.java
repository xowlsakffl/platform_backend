package com.platform.adapter.in.web.partner.onboarding.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.platform.application.partner.command.SignupPartnerOnboardingCommand;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PartnerOnboardingSignupRequest(
	@JsonProperty("partner_name") @NotBlank @Size(max = 255) String partnerName,
	@JsonProperty("manager_name") @NotBlank @Size(max = 255) String managerName,
	@NotBlank @Size(max = 255) String nickname,
	@NotBlank @Email @Size(max = 255) String email,
	@Pattern(regexp = "^(?:|[0-9+()\\-\\s]{7,50})$") String phone,
	@NotBlank @Size(min = 8, max = 72) String password
) {

	public SignupPartnerOnboardingCommand toCommand() {
		return new SignupPartnerOnboardingCommand(
			partnerName,
			managerName,
			nickname,
			email,
			phone,
			password
		);
	}
}
