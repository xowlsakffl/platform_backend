package com.medi.infrastructure.persistence.partner;

import com.medi.domain.partner.PartnerFeature;
import com.medi.domain.partner.PartnerFeatureStatus;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface PartnerFeatureRepository extends
	JpaRepository<PartnerFeature, Long>,
	JpaSpecificationExecutor<PartnerFeature> {

	List<PartnerFeature> findByIdInAndStatus(Collection<Long> ids, PartnerFeatureStatus status);

	List<PartnerFeature> findByCodeInAndStatus(Collection<String> codes, PartnerFeatureStatus status);
}
