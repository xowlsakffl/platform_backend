package com.platform.infrastructure.persistence.account;

import com.platform.domain.account.StaffRole;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StaffRoleRepository extends JpaRepository<StaffRole, Long> {

	Optional<StaffRole> findByName(String name);
}
