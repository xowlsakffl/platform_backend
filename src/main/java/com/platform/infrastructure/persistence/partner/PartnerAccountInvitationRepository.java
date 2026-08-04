package com.platform.infrastructure.persistence.partner;

import com.platform.domain.partner.PartnerAccountInvitation;
import com.platform.domain.partner.PartnerAccountInvitationStatus;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface PartnerAccountInvitationRepository extends JpaRepository<PartnerAccountInvitation, Long> {

	@EntityGraph(attributePaths = "partner")
	List<PartnerAccountInvitation> findByPartner_IdOrderByCreatedAtDescIdDesc(Long partnerId);

	@EntityGraph(attributePaths = "partner")
	List<PartnerAccountInvitation> findByPartner_IdInOrderByCreatedAtDescIdDesc(Collection<Long> partnerIds);

	List<PartnerAccountInvitation> findByPartner_IdAndStatus(
		Long partnerId,
		PartnerAccountInvitationStatus status
	);

	@EntityGraph(attributePaths = "partner")
	List<PartnerAccountInvitation> findByEmailAndStatus(
		String email,
		PartnerAccountInvitationStatus status
	);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	Optional<PartnerAccountInvitation> findForUpdateByIdAndPartner_Id(Long id, Long partnerId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@EntityGraph(attributePaths = "partner")
	Optional<PartnerAccountInvitation> findForUpdateByTokenHash(String tokenHash);

	@EntityGraph(attributePaths = "partner")
	Optional<PartnerAccountInvitation> findByTokenHash(String tokenHash);
}
