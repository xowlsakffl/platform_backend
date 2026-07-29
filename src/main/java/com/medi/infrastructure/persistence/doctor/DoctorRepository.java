package com.medi.infrastructure.persistence.doctor;

import com.medi.domain.doctor.Doctor;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;

public interface DoctorRepository extends JpaRepository<Doctor, Long>, JpaSpecificationExecutor<Doctor> {

	Optional<Doctor> findByIdAndDeletedAtIsNullAndHospital_DeletedAtIsNull(Long id);

	Optional<Doctor> findByIdAndHospital_IdAndDeletedAtIsNullAndHospital_DeletedAtIsNull(Long id, Long hospitalId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	Optional<Doctor> findForUpdateByIdAndDeletedAtIsNull(Long id);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	Optional<Doctor> findForUpdateByIdAndHospital_IdAndDeletedAtIsNullAndHospital_DeletedAtIsNull(
		Long id,
		Long hospitalId
	);

	boolean existsByIdAndDeletedAtIsNullAndHospital_DeletedAtIsNull(Long id);

	boolean existsByLicenseNumber(String licenseNumber);

	boolean existsByLicenseNumberAndIdNot(String licenseNumber, Long id);

	List<Doctor> findByHospital_IdAndDeletedAtIsNull(Long hospitalId);

	List<Doctor> findByHospital_IdAndDeletedAtIsNullOrderBySortOrderAscIdAsc(Long hospitalId);
}
