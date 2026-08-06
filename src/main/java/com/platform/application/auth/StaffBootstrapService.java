package com.platform.application.auth;

import com.platform.application.auth.command.BootstrapStaffCommand;
import com.platform.common.error.InternalApplicationException;
import com.platform.domain.account.AccountStaff;
import com.platform.domain.account.StaffRole;
import com.platform.infrastructure.persistence.account.AccountStaffRepository;
import com.platform.infrastructure.persistence.account.StaffRoleRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class StaffBootstrapService {

	private final AccountStaffRepository staffRepository;
	private final StaffRoleRepository roleRepository;
	private final PasswordEncoder passwordEncoder;

	public StaffBootstrapService(
		AccountStaffRepository staffRepository,
		StaffRoleRepository roleRepository,
		PasswordEncoder passwordEncoder
	) {
		this.staffRepository = staffRepository;
		this.roleRepository = roleRepository;
		this.passwordEncoder = passwordEncoder;
	}

	@Transactional
	public boolean ensureStaff(BootstrapStaffCommand command) {
		validate(command);

		String loginId = command.loginId().trim().toLowerCase();
		String email = command.email().trim().toLowerCase();
		String nickname = command.nickname().trim();
		StaffRole role = roleRepository.findByName(command.roleName().trim())
			.orElseThrow(() -> new InternalApplicationException("운영자 역할을 찾을 수 없습니다: " + command.roleName()));

		AccountStaff existingStaff = staffRepository.findByLoginId(loginId).orElse(null);
		if (existingStaff != null) {
			if (!existingStaff.isActive()) {
				throw new InternalApplicationException("같은 이메일의 비활성 운영자 계정이 존재합니다: " + email);
			}
			existingStaff.assignRole(role);
			return false;
		}

		if (staffRepository.findByEmail(email).isPresent()) {
			throw new InternalApplicationException("같은 이메일의 운영자 계정이 존재합니다: " + email);
		}
		if (staffRepository.existsByNickname(nickname)) {
			throw new InternalApplicationException("같은 닉네임의 운영자 계정이 존재합니다: " + nickname);
		}

		AccountStaff staff = AccountStaff.create(
			loginId,
			command.name().trim(),
			nickname,
			email,
			passwordEncoder.encode(command.password())
		);
		staff.assignRole(role);
		staffRepository.save(staff);
		return true;
	}

	private void validate(BootstrapStaffCommand command) {
		if (command == null
			|| !StringUtils.hasText(command.loginId())
			|| !StringUtils.hasText(command.email())
			|| !StringUtils.hasText(command.password())
			|| !StringUtils.hasText(command.name())
			|| !StringUtils.hasText(command.nickname())
			|| !StringUtils.hasText(command.roleName())) {
			throw new InternalApplicationException("운영자 초기화 설정값이 비어 있습니다.");
		}
	}
}
