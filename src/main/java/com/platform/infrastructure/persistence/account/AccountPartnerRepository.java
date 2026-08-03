package com.platform.infrastructure.persistence.account;

import com.platform.domain.account.AccountPartner;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AccountPartnerRepository extends JpaRepository<AccountPartner, Long> {

	boolean existsByEmail(String email);

	boolean existsByNickname(String nickname);

	Optional<AccountPartner> findByEmailAndDeletedAtIsNull(String email);

	Optional<AccountPartner> findByIdAndDeletedAtIsNull(Long id);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	Optional<AccountPartner> findForUpdateByPartner_IdAndDeletedAtIsNull(Long partnerId);

	List<AccountPartner> findByPartner_IdInAndDeletedAtIsNull(Collection<Long> partnerIds);

	@Query("""
		select count(account)
		from AccountPartner account
		join account.partner partner
		where account.deletedAt is null
		  and account.status = com.platform.domain.account.AccountPartnerStatus.ACTIVE
		  and partner.deletedAt is null
		  and partner.status <> com.platform.domain.partner.PartnerStatus.WITHDRAWN
		  and (account.lastLoginAt is null or account.lastLoginAt < :cutoff)
		""")
	long countDormantPartnerAccounts(@Param("cutoff") LocalDateTime cutoff);
}
