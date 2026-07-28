package com.medi.infrastructure.persistence.hospital;

import com.medi.domain.hospital.HospitalFeature;
import com.medi.domain.hospital.HospitalFeatureStatus;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HospitalFeatureRepository extends JpaRepository<HospitalFeature, Long> {

	List<HospitalFeature> findByIdInAndStatus(Collection<Long> ids, HospitalFeatureStatus status);
}
