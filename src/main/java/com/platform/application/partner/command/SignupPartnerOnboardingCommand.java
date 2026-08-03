package com.platform.application.partner.command;

public record SignupPartnerOnboardingCommand(
	String partnerName,
	String managerName,
	String nickname,
	String email,
	String phone,
	String password
) {
}
