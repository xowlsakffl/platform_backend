package com.medi.infrastructure.persistence.account;

import com.medi.domain.account.AccountHospital;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountHospitalRepository extends JpaRepository<AccountHospital, Long> {

	Optional<AccountHospital> findByEmailAndDeletedAtIsNull(String email);

	Optional<AccountHospital> findByIdAndDeletedAtIsNull(Long id);
}
