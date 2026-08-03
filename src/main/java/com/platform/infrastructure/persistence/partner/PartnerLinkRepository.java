package com.platform.infrastructure.persistence.partner;

import com.platform.domain.partner.PartnerLink;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PartnerLinkRepository extends JpaRepository<PartnerLink, Long> {

	List<PartnerLink> findByPartner_IdOrderBySortOrderAscIdAsc(Long partnerId);

	void deleteByPartner_Id(Long partnerId);
}
