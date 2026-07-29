package com.medi.infrastructure.persistence.hospital;

import com.medi.domain.hospital.Hospital;
import com.medi.domain.hospital.HospitalAllowStatus;
import com.medi.domain.hospital.HospitalStatus;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;

public interface HospitalRepository extends JpaRepository<Hospital, Long>, JpaSpecificationExecutor<Hospital> {

	boolean existsByNameAndDeletedAtIsNull(String name);

	boolean existsByIdAndDeletedAtIsNull(Long id);

	@EntityGraph(attributePaths = {"contacts", "businessRegistration", "accountHospital", "features"})
	Optional<Hospital> findByIdAndDeletedAtIsNull(Long id);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	Optional<Hospital> findForUpdateByIdAndDeletedAtIsNull(Long id);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	List<Hospital> findByIdInAndDeletedAtIsNull(Collection<Long> ids);

	long countByDeletedAtIsNull();

	long countByDeletedAtIsNullAndAllowStatus(HospitalAllowStatus allowStatus);

	long countByDeletedAtIsNullAndStatus(HospitalStatus status);
}
