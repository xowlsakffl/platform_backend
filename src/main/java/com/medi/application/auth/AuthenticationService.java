package com.medi.application.auth;

import com.medi.application.auth.command.AuthLoginCommand;
import com.medi.application.auth.command.AuthClientContext;
import com.medi.application.auth.result.AuthActorResult;
import com.medi.application.auth.result.AuthLogoutResult;
import com.medi.application.auth.result.AuthSessionTokenResult;
import com.medi.application.auth.result.AuthTokenResult;
import com.medi.application.auth.result.RotatedAuthSessionResult;
import com.medi.application.cache.StaffSummaryCache;
import com.medi.application.cache.StaffSummaryCacheInvalidator;
import com.medi.common.error.ApiException;
import com.medi.common.error.ErrorCode;
import com.medi.common.error.RefreshTokenReuseException;
import com.medi.common.security.AuthenticatedActor;
import com.medi.common.security.JwtTokenService;
import com.medi.domain.account.AccountActorType;
import com.medi.domain.account.AccountBeauty;
import com.medi.domain.account.AccountPartner;
import com.medi.domain.account.AccountStaff;
import com.medi.domain.account.AccountUser;
import com.medi.infrastructure.persistence.account.AccountBeautyRepository;
import com.medi.infrastructure.persistence.account.AccountPartnerRepository;
import com.medi.infrastructure.persistence.account.AccountStaffRepository;
import com.medi.infrastructure.persistence.account.AccountUserRepository;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthenticationService {

	private final AccountStaffRepository staffRepository;
	private final AccountPartnerRepository partnerRepository;
	private final AccountBeautyRepository beautyRepository;
	private final AccountUserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtTokenService jwtTokenService;
	private final AuthSessionService authSessionService;
	private final LoginAttemptPolicy loginAttemptPolicy;
	private final StaffSummaryCacheInvalidator summaryCacheInvalidator;

	public AuthenticationService(
		AccountStaffRepository staffRepository,
		AccountPartnerRepository partnerRepository,
		AccountBeautyRepository beautyRepository,
		AccountUserRepository userRepository,
		PasswordEncoder passwordEncoder,
		JwtTokenService jwtTokenService,
		AuthSessionService authSessionService,
		LoginAttemptPolicy loginAttemptPolicy,
		StaffSummaryCacheInvalidator summaryCacheInvalidator
	) {
		this.staffRepository = staffRepository;
		this.partnerRepository = partnerRepository;
		this.beautyRepository = beautyRepository;
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.jwtTokenService = jwtTokenService;
		this.authSessionService = authSessionService;
		this.loginAttemptPolicy = loginAttemptPolicy;
		this.summaryCacheInvalidator = summaryCacheInvalidator;
	}

	@Transactional
	public AuthSessionTokenResult login(AccountActorType actorType, AuthLoginCommand command) {
		String email = normalizeEmail(command.email());
		String ipAddress = command.client().ipAddress();
		loginAttemptPolicy.assertAllowed(actorType, email, ipAddress);

		AuthenticatedActor actor;
		try {
			actor = switch (actorType) {
				case STAFF -> loginStaff(email, command.password());
				case PARTNER -> loginPartner(email, command.password());
				case BEAUTY -> loginBeauty(email, command.password());
				case USER -> loginUser(email, command.password());
			};
		} catch (ApiException exception) {
			loginAttemptPolicy.recordFailure(actorType, email, ipAddress);
			throw exception;
		}

		loginAttemptPolicy.recordSuccess(actorType, email, ipAddress);
		RotatedAuthSessionResult session = authSessionService.create(
			actorType,
			actor.accountId(),
			command.keepLoggedIn(),
			command.client()
		);
		AuthenticatedActor sessionActor = actor.withSessionId(session.sessionId());
		return sessionToken(sessionActor, session);
	}

	@Transactional(noRollbackFor = RefreshTokenReuseException.class)
	public AuthSessionTokenResult refresh(
		AccountActorType expectedActorType,
		String refreshToken,
		AuthClientContext client
	) {
		RotatedAuthSessionResult session = authSessionService.rotate(expectedActorType, refreshToken, client);
		AuthenticatedActor actor = actorById(
			session.actorType(),
			session.accountId(),
			session.sessionId()
		);
		return sessionToken(actor, session);
	}

	@Transactional(readOnly = true)
	public AuthenticatedActor authenticate(AuthenticatedActor tokenActor) {
		if (!authSessionService.isActive(tokenActor.sessionId(), tokenActor.actorType(), tokenActor.accountId())) {
			throw unauthorized();
		}
		return actorById(tokenActor.actorType(), tokenActor.accountId(), tokenActor.sessionId());
	}

	private AuthenticatedActor actorById(AccountActorType actorType, Long accountId, String sessionId) {
		return switch (actorType) {
			case STAFF -> actorFromStaff(staffRepository.findForAuthentication(accountId)
				.filter(AccountStaff::isActive)
				.orElseThrow(this::unauthorized), sessionId);
			case PARTNER -> actorFromPartner(partnerRepository.findByIdAndDeletedAtIsNull(accountId)
				.filter(AccountPartner::isActive)
				.orElseThrow(this::unauthorized), sessionId);
			case BEAUTY -> actorFromBeauty(beautyRepository.findByIdAndDeletedAtIsNull(accountId)
				.filter(AccountBeauty::isActive)
				.orElseThrow(this::unauthorized), sessionId);
			case USER -> actorFromUser(userRepository.findByIdAndDeletedAtIsNull(accountId)
				.filter(AccountUser::isActive)
				.orElseThrow(this::unauthorized), sessionId);
		};
	}

	@Transactional(readOnly = true)
	public AuthActorResult me(AccountActorType expectedActorType, AuthenticatedActor actor) {
		AuthenticatedActor authenticatedActor = requireActor(expectedActorType, actor);
		return AuthActorResult.from(authenticatedActor);
	}

	public AuthLogoutResult logout(AccountActorType expectedActorType, AuthenticatedActor actor, String accessToken) {
		AuthenticatedActor authenticatedActor = requireActor(expectedActorType, actor);
		authSessionService.revoke(
			authenticatedActor.sessionId(),
			authenticatedActor.actorType(),
			authenticatedActor.accountId(),
			"LOGOUT"
		);
		if (accessToken == null) {
			throw unauthorized();
		}
		jwtTokenService.revoke(accessToken);
		return new AuthLogoutResult(true);
	}

	public AuthLogoutResult logoutAll(AccountActorType expectedActorType, AuthenticatedActor actor, String accessToken) {
		AuthenticatedActor authenticatedActor = requireActor(expectedActorType, actor);
		authSessionService.revokeAll(authenticatedActor.actorType(), authenticatedActor.accountId(), "LOGOUT_ALL");
		if (accessToken != null) {
			jwtTokenService.revoke(accessToken);
		}
		return new AuthLogoutResult(true);
	}

	private AuthenticatedActor loginStaff(String email, String password) {
		AccountStaff staff = staffRepository.findByEmailAndDeletedAtIsNull(email)
			.orElseThrow(this::invalidCredentials);
		assertPassword(password, staff.password());
		assertUsable(staff.isActive());
		staff.markLoggedIn();
		return actorFromStaff(staff, null);
	}

	private AuthenticatedActor loginPartner(String email, String password) {
		AccountPartner partner = partnerRepository.findByEmailAndDeletedAtIsNull(email)
			.orElseThrow(this::invalidCredentials);
		assertPassword(password, partner.password());
		assertUsable(partner.isActive());
		partner.markLoggedIn();
		summaryCacheInvalidator.forgetAfterCommit(StaffSummaryCache.PARTNER);
		return actorFromPartner(partner, null);
	}

	private AuthenticatedActor loginBeauty(String email, String password) {
		AccountBeauty beauty = beautyRepository.findByEmailAndDeletedAtIsNull(email)
			.orElseThrow(this::invalidCredentials);
		assertPassword(password, beauty.password());
		assertUsable(beauty.isActive());
		beauty.markLoggedIn();
		return actorFromBeauty(beauty, null);
	}

	private AuthenticatedActor loginUser(String email, String password) {
		AccountUser user = userRepository.findByEmailAndDeletedAtIsNull(email)
			.orElseThrow(this::invalidCredentials);
		assertPassword(password, user.password());
		assertUsable(user.isActive());
		user.markLoggedIn();
		return actorFromUser(user, null);
	}

	private AuthenticatedActor actorFromStaff(AccountStaff staff, String sessionId) {
		return new AuthenticatedActor(
			AccountActorType.STAFF,
			staff.id(),
			null,
			null,
			sessionId,
			staff.email(),
			staff.name(),
			staff.nickname(),
			staff.permissionCodes()
		);
	}

	private AuthenticatedActor actorFromPartner(AccountPartner partner, String sessionId) {
		return new AuthenticatedActor(
			AccountActorType.PARTNER,
			partner.id(),
			partner.partnerId(),
			null,
			sessionId,
			partner.email(),
			partner.name(),
			partner.nickname(),
			Set.of()
		);
	}

	private AuthenticatedActor actorFromBeauty(AccountBeauty beauty, String sessionId) {
		return new AuthenticatedActor(
			AccountActorType.BEAUTY,
			beauty.id(),
			null,
			beauty.beautyId(),
			sessionId,
			beauty.email(),
			beauty.name(),
			beauty.nickname(),
			Set.of()
		);
	}

	private AuthenticatedActor actorFromUser(AccountUser user, String sessionId) {
		return new AuthenticatedActor(
			AccountActorType.USER,
			user.id(),
			null,
			null,
			sessionId,
			user.email(),
			user.name(),
			user.nickname(),
			Set.of()
		);
	}

	private AuthenticatedActor requireActor(AccountActorType expectedActorType, AuthenticatedActor actor) {
		if (actor == null) {
			throw unauthorized();
		}
		if (actor.actorType() != expectedActorType) {
			throw new ApiException(ErrorCode.FORBIDDEN);
		}
		return actor;
	}

	private AuthSessionTokenResult sessionToken(
		AuthenticatedActor actor,
		RotatedAuthSessionResult session
	) {
		AuthTokenResult token = new AuthTokenResult(
			"Bearer",
			jwtTokenService.issue(actor),
			jwtTokenService.accessTokenTtlSeconds(),
			AuthActorResult.from(actor)
		);
		return new AuthSessionTokenResult(
			token,
			session.refreshToken(),
			session.refreshExpiresIn(),
			session.persistent()
		);
	}

	private void assertPassword(String rawPassword, String encodedPassword) {
		if (rawPassword == null
			|| rawPassword.getBytes(StandardCharsets.UTF_8).length > 72
			|| !passwordEncoder.matches(rawPassword, encodedPassword)) {
			throw invalidCredentials();
		}
	}

	private void assertUsable(boolean active) {
		if (!active) {
			throw new ApiException(ErrorCode.FORBIDDEN, "사용할 수 없는 계정입니다.");
		}
	}

	private String normalizeEmail(String email) {
		if (email == null || email.isBlank()) {
			throw invalidCredentials();
		}
		return email.trim().toLowerCase();
	}

	private ApiException invalidCredentials() {
		return new ApiException(ErrorCode.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다.");
	}

	private ApiException unauthorized() {
		return new ApiException(ErrorCode.UNAUTHORIZED);
	}
}
