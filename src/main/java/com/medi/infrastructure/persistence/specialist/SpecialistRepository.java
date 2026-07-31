package com.medi.infrastructure.persistence.specialist;

import com.medi.domain.specialist.Specialist;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;

public interface SpecialistRepository extends JpaRepository<Specialist, Long>, JpaSpecificationExecutor<Specialist> {

	Optional<Specialist> findByIdAndDeletedAtIsNullAndPartner_DeletedAtIsNull(Long id);

	Optional<Specialist> findByIdAndPartner_IdAndDeletedAtIsNullAndPartner_DeletedAtIsNull(Long id, Long partnerId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	Optional<Specialist> findForUpdateByIdAndDeletedAtIsNull(Long id);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	Optional<Specialist> findForUpdateByIdAndPartner_IdAndDeletedAtIsNullAndPartner_DeletedAtIsNull(
		Long id,
		Long partnerId
	);

	boolean existsByIdAndDeletedAtIsNullAndPartner_DeletedAtIsNull(Long id);

	boolean existsByLicenseNumber(String licenseNumber);

	boolean existsByLicenseNumberAndIdNot(String licenseNumber, Long id);

	List<Specialist> findByPartner_IdAndDeletedAtIsNull(Long partnerId);

	List<Specialist> findByPartner_IdAndDeletedAtIsNullOrderBySortOrderAscIdAsc(Long partnerId);
}
