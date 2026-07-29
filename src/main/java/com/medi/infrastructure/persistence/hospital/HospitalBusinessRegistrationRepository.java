package com.medi.infrastructure.persistence.hospital;

import com.medi.domain.hospital.HospitalBusinessRegistration;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;

public interface HospitalBusinessRegistrationRepository extends JpaRepository<HospitalBusinessRegistration, Long> {

	boolean existsByBusinessNumber(String businessNumber);

	Optional<HospitalBusinessRegistration> findByBusinessNumber(String businessNumber);

	boolean existsByIdAndHospital_DeletedAtIsNull(Long id);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	Optional<HospitalBusinessRegistration> findLockedByIdAndHospital_DeletedAtIsNull(Long id);
}
