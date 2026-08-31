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

	Optional<Partner> findByBusinessRegistration_BusinessNumber(String businessNumber);

	boolean existsByIdAndDeletedAtIsNull(Long id);

	@EntityGraph(attributePaths = {
		"contacts",
		"businessRegistration",
		"accountPartner",
		"assignedStaff",
		"reviewerStaff",
		"features"
	})
	Optional<Partner> findByIdAndDeletedAtIsNull(Long id);

	@Override
	@EntityGraph(attributePaths = {"assignedStaff", "reviewerStaff"})
	Page<Partner> findAll(Specification<Partner> specification, Pageable pageable);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	Optional<Partner> findForUpdateByIdAndDeletedAtIsNull(Long id);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	List<Partner> findByIdInAndDeletedAtIsNull(Collection<Long> ids);

	long countByDeletedAtIsNull();

	long countByDeletedAtIsNullAndAllowStatus(PartnerAllowStatus allowStatus);

	long countByDeletedAtIsNullAndStatus(PartnerStatus status);

	long countByDeletedAtIsNullAndAllowStatusIn(Collection<PartnerAllowStatus> allowStatuses);

	@Query("""
		select count(partner)
		from Partner partner
		where partner.status = com.platform.domain.partner.PartnerStatus.WITHDRAWN
		   or partner.deletedAt is not null
		""")
	long countWithdrawnOrDeleted();

	@Query("""
		select partner.id as partnerId, contact.value as email
		from Partner partner
		join partner.contacts contact
		where partner.id in :partnerIds
		  and contact.contactType = com.platform.domain.partner.PartnerContactType.REPRESENTATIVE_EMAIL
		  and contact.active = true
		  and contact.deletedAt is null
		""")
	List<PartnerRepresentativeEmail> findRepresentativeEmailsByPartnerIds(Collection<Long> partnerIds);
}
