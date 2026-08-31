package com.platform.infrastructure.persistence.specialist;

import com.platform.domain.specialist.Specialist;
import com.platform.infrastructure.persistence.partner.PartnerResourceCount;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface SpecialistRepository extends JpaRepository<Specialist, Long>, JpaSpecificationExecutor<Specialist> {

	@Override
	@EntityGraph(attributePaths = {"partner", "reviewerStaff"})
	Page<Specialist> findAll(Specification<Specialist> specification, Pageable pageable);

	@Query("""
		select specialist.partner.id as partnerId, count(specialist.id) as itemCount
		from Specialist specialist
		where specialist.partner.id in :partnerIds
		  and specialist.deletedAt is null
		group by specialist.partner.id
		""")
	List<PartnerResourceCount> countActiveByPartnerIds(Collection<Long> partnerIds);

	@EntityGraph(attributePaths = {"partner", "reviewerStaff"})
	Optional<Specialist> findByIdAndDeletedAtIsNullAndPartner_DeletedAtIsNull(Long id);

	@EntityGraph(attributePaths = {"partner", "reviewerStaff"})
	Optional<Specialist> findByIdAndPartner_IdAndDeletedAtIsNullAndPartner_DeletedAtIsNull(Long id, Long partnerId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	Optional<Specialist> findForUpdateByIdAndDeletedAtIsNull(Long id);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	Optional<Specialist> findForUpdateByIdAndPartner_IdAndDeletedAtIsNullAndPartner_DeletedAtIsNull(
		Long id,
		Long partnerId
	);

	boolean existsByIdAndDeletedAtIsNullAndPartner_DeletedAtIsNull(Long id);

	List<Specialist> findByPartner_IdAndDeletedAtIsNull(Long partnerId);

	@EntityGraph(attributePaths = {"partner", "reviewerStaff"})
	List<Specialist> findByPartner_IdAndDeletedAtIsNullOrderBySortOrderAscIdAsc(Long partnerId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@EntityGraph(attributePaths = {"partner", "reviewerStaff"})
	List<Specialist> findForUpdateByPartner_IdAndDeletedAtIsNullOrderBySortOrderAscIdAsc(Long partnerId);

	@Query("select coalesce(max(specialist.sortOrder), -1) + 1 from Specialist specialist where specialist.partner.id = :partnerId")
	int nextSortOrder(Long partnerId);
}
