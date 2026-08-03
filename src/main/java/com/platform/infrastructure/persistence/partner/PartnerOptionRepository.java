package com.platform.infrastructure.persistence.partner;

import com.platform.domain.partner.PartnerOption;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface PartnerOptionRepository extends JpaRepository<PartnerOption, Long> {

	List<PartnerOption> findByPartner_IdAndDeletedAtIsNullOrderBySortOrderAscIdAsc(Long partnerId);

	long countByPartner_IdAndDeletedAtIsNull(Long partnerId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	Optional<PartnerOption> findForUpdateByIdAndPartner_IdAndDeletedAtIsNull(Long id, Long partnerId);
}
