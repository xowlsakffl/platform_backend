package com.platform.infrastructure.persistence.partner;

import com.platform.domain.partner.PartnerBusinessRegistration;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;

public interface PartnerBusinessRegistrationRepository extends JpaRepository<PartnerBusinessRegistration, Long> {

	boolean existsByBusinessNumber(String businessNumber);

	Optional<PartnerBusinessRegistration> findByBusinessNumber(String businessNumber);

	boolean existsByIdAndPartner_DeletedAtIsNull(Long id);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	Optional<PartnerBusinessRegistration> findLockedByIdAndPartner_DeletedAtIsNull(Long id);
}
