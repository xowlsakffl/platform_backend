package com.medi.infrastructure.persistence.account;

import com.medi.domain.account.StaffRole;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StaffRoleRepository extends JpaRepository<StaffRole, Long> {

	Optional<StaffRole> findByName(String name);
}
