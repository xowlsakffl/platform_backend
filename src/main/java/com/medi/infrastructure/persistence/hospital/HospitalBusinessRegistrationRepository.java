package com.medi.infrastructure.persistence.hospital;

import com.medi.domain.hospital.HospitalBusinessRegistration;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HospitalBusinessRegistrationRepository extends JpaRepository<HospitalBusinessRegistration, Long> {

	boolean existsByBusinessNumber(String businessNumber);

	Optional<HospitalBusinessRegistration> findByBusinessNumber(String businessNumber);
}
