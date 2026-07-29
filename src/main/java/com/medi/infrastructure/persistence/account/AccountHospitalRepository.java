package com.medi.infrastructure.persistence.account;

import com.medi.domain.account.AccountHospital;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AccountHospitalRepository extends JpaRepository<AccountHospital, Long> {

	Optional<AccountHospital> findByEmailAndDeletedAtIsNull(String email);

	Optional<AccountHospital> findByIdAndDeletedAtIsNull(Long id);

	@Query("""
		select count(account)
		from AccountHospital account
		join account.hospital hospital
		where account.deletedAt is null
		  and account.status <> com.medi.domain.account.AccountHospitalStatus.WITHDRAWN
		  and hospital.deletedAt is null
		  and hospital.status <> com.medi.domain.hospital.HospitalStatus.WITHDRAWN
		  and (account.lastLoginAt is null or account.lastLoginAt < :cutoff)
		""")
	long countDormantHospitalAccounts(@Param("cutoff") LocalDateTime cutoff);
}
