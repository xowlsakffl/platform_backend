package com.platform.infrastructure.persistence.partner;

import com.platform.domain.partner.Partner;
import com.platform.domain.partner.PartnerAllowStatus;
import com.platform.domain.partner.PartnerStatus;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.domain.Specification;

public interface PartnerRepository extends JpaRepository<Partner, Long>, JpaSpecificationExecutor<Partner> {

	boolean existsByName(String name);

	Optional<Partner> findByName(String name);

	boolean existsByIdAndDeletedAtIsNull(Long id);

	@EntityGraph(attributePaths = {
		"contacts",
		"businessRegistration",
		"accountPartner",
		"assignedStaff",
		"features"
	})
	Optional<Partner> findByIdAndDeletedAtIsNull(Long id);

	@Override
	@EntityGraph(attributePaths = "assignedStaff")
	Page<Partner> findAll(Specification<Partner> specification, Pageable pageable);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	Optional<Partner> findForUpdateByIdAndDeletedAtIsNull(Long id);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	List<Partner> findByIdInAndDeletedAtIsNull(Collection<Long> ids);

	long countByDeletedAtIsNull();

	long countByDeletedAtIsNullAndAllowStatus(PartnerAllowStatus allowStatus);

	long countByDeletedAtIsNullAndStatus(PartnerStatus status);

	long countByAllowStatus(PartnerAllowStatus allowStatus);

	@Query("""
		select count(partner)
		from Partner partner
		where partner.status = com.platform.domain.partner.PartnerStatus.WITHDRAWN
		   or partner.deletedAt is not null
		""")
	long countWithdrawnOrDeleted();
}
