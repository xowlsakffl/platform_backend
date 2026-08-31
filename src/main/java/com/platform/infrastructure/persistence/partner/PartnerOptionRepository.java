package com.platform.infrastructure.persistence.partner;

import com.platform.domain.partner.PartnerOption;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface PartnerOptionRepository extends JpaRepository<PartnerOption, Long> {

	@Query("""
		select partnerOption.partner.id as partnerId, count(partnerOption.id) as itemCount
		from PartnerOption partnerOption
		where partnerOption.partner.id in :partnerIds
		  and partnerOption.deletedAt is null
		group by partnerOption.partner.id
		""")
	List<PartnerResourceCount> countActiveByPartnerIds(Collection<Long> partnerIds);

	List<PartnerOption> findByPartner_IdAndDeletedAtIsNullOrderBySortOrderAscIdAsc(Long partnerId);

	List<PartnerOption> findByIdInAndPartner_IdAndDeletedAtIsNull(Collection<Long> ids, Long partnerId);

	long countByPartner_IdAndDeletedAtIsNull(Long partnerId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	Optional<PartnerOption> findForUpdateByIdAndPartner_IdAndDeletedAtIsNull(Long id, Long partnerId);
}
