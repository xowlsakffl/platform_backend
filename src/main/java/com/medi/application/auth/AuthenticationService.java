package com.medi.application.auth;

import com.medi.application.auth.command.AuthLoginCommand;
import com.medi.application.auth.result.AuthActorResult;
import com.medi.application.auth.result.AuthLogoutResult;
import com.medi.application.auth.result.AuthTokenResult;
import com.medi.common.error.ApiException;
import com.medi.common.error.ErrorCode;
import com.medi.common.security.AuthenticatedActor;
import com.medi.common.security.JwtTokenService;
import com.medi.domain.account.AccountActorType;
import com.medi.domain.account.AccountBeauty;
import com.medi.domain.account.AccountHospital;
import com.medi.domain.account.AccountStaff;
import com.medi.domain.account.AccountUser;
import com.medi.infrastructure.persistence.account.AccountBeautyRepository;
import com.medi.infrastructure.persistence.account.AccountHospitalRepository;
import com.medi.infrastructure.persistence.account.AccountStaffRepository;
import com.medi.infrastructure.persistence.account.AccountUserRepository;
import java.util.Set;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthenticationService {

	private final AccountStaffRepository staffRepository;
	private final AccountHospitalRepository hospitalRepository;
	private final AccountBeautyRepository beautyRepository;
	private final AccountUserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtTokenService jwtTokenService;

	public AuthenticationService(
		AccountStaffRepository staffRepository,
		AccountHospitalRepository hospitalRepository,
		AccountBeautyRepository beautyRepository,
		AccountUserRepository userRepository,
		PasswordEncoder passwordEncoder,
		JwtTokenService jwtTokenService
	) {
		this.staffRepository = staffRepository;
		this.hospitalRepository = hospitalRepository;
		this.beautyRepository = beautyRepository;
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.jwtTokenService = jwtTokenService;
	}

	@Transactional
	public AuthTokenResult login(AccountActorType actorType, AuthLoginCommand command) {
		AuthenticatedActor actor = switch (actorType) {
			case STAFF -> loginStaff(command);
			case HOSPITAL -> loginHospital(command);
			case BEAUTY -> loginBeauty(command);
			case USER -> loginUser(command);
		};

		return new AuthTokenResult(
			"Bearer",
			jwtTokenService.issue(actor),
			jwtTokenService.accessTokenTtlSeconds(),
			AuthActorResult.from(actor)
		);
	}

	@Transactional(readOnly = true)
	public AuthenticatedActor authenticate(AuthenticatedActor tokenActor) {
		return switch (tokenActor.actorType()) {
			case STAFF -> actorFromStaff(staffRepository.findForAuthentication(tokenActor.accountId())
				.filter(AccountStaff::isActive)
				.orElseThrow(this::unauthorized));
			case HOSPITAL -> actorFromHospital(hospitalRepository.findByIdAndDeletedAtIsNull(tokenActor.accountId())
				.filter(AccountHospital::isActive)
				.orElseThrow(this::unauthorized));
			case BEAUTY -> actorFromBeauty(beautyRepository.findByIdAndDeletedAtIsNull(tokenActor.accountId())
				.filter(AccountBeauty::isActive)
				.orElseThrow(this::unauthorized));
			case USER -> actorFromUser(userRepository.findByIdAndDeletedAtIsNull(tokenActor.accountId())
				.filter(AccountUser::isActive)
				.orElseThrow(this::unauthorized));
		};
	}

	@Transactional(readOnly = true)
	public AuthActorResult me(AccountActorType expectedActorType, AuthenticatedActor actor) {
		AuthenticatedActor authenticatedActor = requireActor(expectedActorType, actor);
		return AuthActorResult.from(authenticatedActor);
	}

	public AuthLogoutResult logout(AccountActorType expectedActorType, AuthenticatedActor actor) {
		requireActor(expectedActorType, actor);
		return new AuthLogoutResult(true);
	}

	private AuthenticatedActor loginStaff(AuthLoginCommand command) {
		AccountStaff staff = staffRepository.findByEmailAndDeletedAtIsNull(normalizeEmail(command.email()))
			.orElseThrow(this::invalidCredentials);
		assertPassword(command.password(), staff.password());
		assertUsable(staff.isActive());
		staff.markLoggedIn();
		return actorFromStaff(staff);
	}

	private AuthenticatedActor loginHospital(AuthLoginCommand command) {
		AccountHospital hospital = hospitalRepository.findByEmailAndDeletedAtIsNull(normalizeEmail(command.email()))
			.orElseThrow(this::invalidCredentials);
		assertPassword(command.password(), hospital.password());
		assertUsable(hospital.isActive());
		hospital.markLoggedIn();
		return actorFromHospital(hospital);
	}

	private AuthenticatedActor loginBeauty(AuthLoginCommand command) {
		AccountBeauty beauty = beautyRepository.findByEmailAndDeletedAtIsNull(normalizeEmail(command.email()))
			.orElseThrow(this::invalidCredentials);
		assertPassword(command.password(), beauty.password());
		assertUsable(beauty.isActive());
		beauty.markLoggedIn();
		return actorFromBeauty(beauty);
	}

	private AuthenticatedActor loginUser(AuthLoginCommand command) {
		AccountUser user = userRepository.findByEmailAndDeletedAtIsNull(normalizeEmail(command.email()))
			.orElseThrow(this::invalidCredentials);
		assertPassword(command.password(), user.password());
		assertUsable(user.isActive());
		user.markLoggedIn();
		return actorFromUser(user);
	}

	private AuthenticatedActor actorFromStaff(AccountStaff staff) {
		return new AuthenticatedActor(
			AccountActorType.STAFF,
			staff.id(),
			null,
			null,
			staff.email(),
			staff.name(),
			staff.nickname(),
			staff.permissionCodes()
		);
	}

	private AuthenticatedActor actorFromHospital(AccountHospital hospital) {
		return new AuthenticatedActor(
			AccountActorType.HOSPITAL,
			hospital.id(),
			hospital.hospitalId(),
			null,
			hospital.email(),
			hospital.name(),
			hospital.nickname(),
			Set.of()
		);
	}

	private AuthenticatedActor actorFromBeauty(AccountBeauty beauty) {
		return new AuthenticatedActor(
			AccountActorType.BEAUTY,
			beauty.id(),
			null,
			beauty.beautyId(),
			beauty.email(),
			beauty.name(),
			beauty.nickname(),
			Set.of()
		);
	}

	private AuthenticatedActor actorFromUser(AccountUser user) {
		return new AuthenticatedActor(
			AccountActorType.USER,
			user.id(),
			null,
			null,
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

	private void assertPassword(String rawPassword, String encodedPassword) {
		if (rawPassword == null || !passwordEncoder.matches(rawPassword, encodedPassword)) {
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
