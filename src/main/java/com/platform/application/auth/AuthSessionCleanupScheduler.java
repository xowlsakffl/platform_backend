package com.platform.application.auth;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AuthSessionCleanupScheduler {

	private final AuthSessionService authSessionService;
	private final PasswordResetTokenService passwordResetTokenService;

	public AuthSessionCleanupScheduler(
		AuthSessionService authSessionService,
		PasswordResetTokenService passwordResetTokenService
	) {
		this.authSessionService = authSessionService;
		this.passwordResetTokenService = passwordResetTokenService;
	}

	@Scheduled(cron = "${app.auth.session.cleanup-cron:0 20 4 * * *}")
	public void cleanup() {
		authSessionService.cleanup();
		passwordResetTokenService.cleanup();
	}
}
