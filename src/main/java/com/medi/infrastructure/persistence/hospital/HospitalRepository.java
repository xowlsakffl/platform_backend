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
import org.springframework.data.jpa.repository.Query;

public interface HospitalRepository extends JpaRepository<Hospital, Long>, JpaSpecificationExecutor<Hospital> {

	boolean existsByNameAndDeletedAtIsNull(String name);

	boolean existsByIdAndDeletedAtIsNull(Long id);

	@EntityGraph(attributePaths = {
		"contacts",
		"businessRegistration",
		"accountHospital",
		"features",
		"interpretationLanguages"
	})
	Optional<Hospital> findByIdAndDeletedAtIsNull(Long id);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	Optional<Hospital> findForUpdateByIdAndDeletedAtIsNull(Long id);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	List<Hospital> findByIdInAndDeletedAtIsNull(Collection<Long> ids);

	long countByDeletedAtIsNull();

	long countByDeletedAtIsNullAndAllowStatus(HospitalAllowStatus allowStatus);

	long countByDeletedAtIsNullAndStatus(HospitalStatus status);

	long countByAllowStatus(HospitalAllowStatus allowStatus);

	@Query("""
		select count(hospital)
		from Hospital hospital
		where hospital.status = com.medi.domain.hospital.HospitalStatus.WITHDRAWN
		   or hospital.deletedAt is not null
		""")
	long countWithdrawnOrDeleted();
}
