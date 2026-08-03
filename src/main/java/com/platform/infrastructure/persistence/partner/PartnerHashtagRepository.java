package com.platform.infrastructure.persistence.partner;

import com.platform.domain.partner.PartnerHashtag;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PartnerHashtagRepository extends JpaRepository<PartnerHashtag, Long> {

	List<PartnerHashtag> findByPartner_IdOrderBySortOrderAscIdAsc(Long partnerId);

	void deleteByPartner_Id(Long partnerId);
}
