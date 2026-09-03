package com.platform.application.auth;

import com.platform.application.auth.command.RegisterPartnerAccountCommand;
import com.platform.common.error.ApiException;
import com.platform.common.error.ErrorCode;
import com.platform.domain.account.AccountPartner;
import com.platform.domain.account.AccountPartnerStatus;
import com.platform.infrastructure.persistence.account.AccountPartnerRepository;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class PartnerAccountRegistrationService {

	private final AccountPartnerRepository accountRepository;
	private final PasswordEncoder passwordEncoder;

	public PartnerAccountRegistrationService(
		AccountPartnerRepository accountRepository,
		PasswordEncoder passwordEncoder
	) {
		this.accountRepository = accountRepository;
		this.passwordEncoder = passwordEncoder;
	}

	@Transactional
	public AccountPartner register(RegisterPartnerAccountCommand command) {
		String name = requireText(command.name(), "이름을 입력해주세요.");
		if (name.length() > 50) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "이름은 50자 이하로 입력해주세요.");
		}
		String loginId = normalizeLoginId(command.loginId());
		String email = requireText(command.email(), "이메일을 입력해주세요.").toLowerCase(Locale.ROOT);
		String password = requireText(command.password(), "비밀번호를 입력해주세요.");
		validatePassword(password);
		if (accountRepository.existsByLoginId(loginId)) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "이미 사용 중인 아이디입니다.");
		}
		if (accountRepository.existsByEmail(email)) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "이미 가입된 이메일입니다.");
		}

		try {
			return accountRepository.saveAndFlush(AccountPartner.create(
				name,
				loginId,
				email,
				trimToNull(command.phone()),
				passwordEncoder.encode(password),
				AccountPartnerStatus.ACTIVE
			));
		} catch (DataIntegrityViolationException exception) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "이미 사용 중인 아이디 또는 이메일입니다.");
		}
	}

	private void validatePassword(String password) {
		if (password.length() < 8 || password.getBytes(StandardCharsets.UTF_8).length > 72) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "비밀번호는 8자 이상, 72바이트 이하로 입력해주세요.");
		}
	}

	private String normalizeLoginId(String value) {
		String loginId = requireText(value, "아이디를 입력해주세요.").toLowerCase(Locale.ROOT);
		if (!loginId.matches("^[a-z0-9][a-z0-9._-]{3,29}$")) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "아이디 형식이 올바르지 않습니다.");
		}
		return loginId;
	}

	private String requireText(String value, String message) {
		if (!StringUtils.hasText(value)) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, message);
		}
		return value.trim();
	}

	private String trimToNull(String value) {
		return StringUtils.hasText(value) ? value.trim() : null;
	}
}
