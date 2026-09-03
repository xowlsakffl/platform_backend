package com.platform.infrastructure.persistence.partner;

import com.platform.domain.partner.PartnerMembership;
import com.platform.domain.partner.PartnerMembershipStatus;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PartnerMembershipRepository extends JpaRepository<PartnerMembership, Long> {

	boolean existsByAccountPartner_IdAndPartner_IdAndStatus(
		Long accountPartnerId,
		Long partnerId,
		PartnerMembershipStatus status
	);

	boolean existsByPartner_IdAndStatus(Long partnerId, PartnerMembershipStatus status);

	@Query("""
		select membership
		from PartnerMembership membership
		join fetch membership.partner partner
		where membership.accountPartner.id = :accountPartnerId
		  and membership.status = :status
		  and partner.deletedAt is null
		order by membership.createdAt asc, membership.id asc
		""")
	List<PartnerMembership> findAllForAccount(
		@Param("accountPartnerId") Long accountPartnerId,
		@Param("status") PartnerMembershipStatus status
	);

	@Query("""
		select membership
		from PartnerMembership membership
		join fetch membership.partner partner
		where membership.accountPartner.id in :accountPartnerIds
		  and membership.status = :status
		  and partner.deletedAt is null
		order by membership.accountPartner.id asc, partner.name asc, partner.id asc
		""")
	List<PartnerMembership> findAllForAccountIds(
		@Param("accountPartnerIds") Collection<Long> accountPartnerIds,
		@Param("status") PartnerMembershipStatus status
	);

	@Query("""
		select membership
		from PartnerMembership membership
		join fetch membership.accountPartner account
		where membership.partner.id in :partnerIds
		  and membership.status = :status
		  and account.deletedAt is null
		order by membership.id asc
		""")
	List<PartnerMembership> findAllForPartnerIds(
		@Param("partnerIds") Collection<Long> partnerIds,
		@Param("status") PartnerMembershipStatus status
	);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
		select membership
		from PartnerMembership membership
		join fetch membership.accountPartner account
		where membership.partner.id = :partnerId
		  and membership.status = :status
		  and account.deletedAt is null
		order by membership.id asc
		""")
	List<PartnerMembership> findAllForPartnerForUpdate(
		@Param("partnerId") Long partnerId,
		@Param("status") PartnerMembershipStatus status
	);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
		select membership
		from PartnerMembership membership
		where membership.accountPartner.id = :accountPartnerId
		  and membership.partner.id = :partnerId
		""")
	Optional<PartnerMembership> findForUpdate(
		@Param("accountPartnerId") Long accountPartnerId,
		@Param("partnerId") Long partnerId
	);
}
