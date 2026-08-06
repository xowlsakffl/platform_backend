package com.platform.application.partner.command;

public record SignupPartnerOnboardingCommand(
	String partnerName,
	String loginId,
	String email,
	String phone,
	String password
) {
}
