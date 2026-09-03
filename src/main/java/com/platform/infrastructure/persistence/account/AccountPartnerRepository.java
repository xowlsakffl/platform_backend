package com.platform.infrastructure.persistence.account;

import com.platform.domain.account.AccountPartner;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AccountPartnerRepository extends JpaRepository<AccountPartner, Long>, JpaSpecificationExecutor<AccountPartner> {

	boolean existsByEmail(String email);

	boolean existsByLoginId(String loginId);

	Optional<AccountPartner> findByEmailAndDeletedAtIsNull(String email);

	Optional<AccountPartner> findByLoginIdAndDeletedAtIsNull(String loginId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
		select account
		from AccountPartner account
		where account.loginId = :loginId
		  and account.deletedAt is null
		""")
	Optional<AccountPartner> findForUpdateByLoginIdAndDeletedAtIsNull(@Param("loginId") String loginId);

	Optional<AccountPartner> findByIdAndDeletedAtIsNull(Long id);

	@Query("""
		select account
		from AccountPartner account
		where account.deletedAt is null
		  and account.status = com.platform.domain.account.AccountPartnerStatus.ACTIVE
		  and lower(account.loginId) like :query
		order by account.loginId asc, account.id asc
		""")
	List<AccountPartner> searchActiveByLoginId(
		@Param("query") String query,
		Pageable pageable
	);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
		select account
		from AccountPartner account
		where account.id = :accountId
		  and account.deletedAt is null
		""")
	Optional<AccountPartner> findForUpdateByIdAndDeletedAtIsNull(@Param("accountId") Long accountId);

	@Query("""
		select count(account)
		from AccountPartner account
		where account.deletedAt is null
		  and account.status = com.platform.domain.account.AccountPartnerStatus.ACTIVE
		  and (account.lastLoginAt is null or account.lastLoginAt < :cutoff)
		""")
	long countDormantPartnerAccounts(@Param("cutoff") LocalDateTime cutoff);
}
