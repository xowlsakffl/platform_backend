package com.platform.infrastructure.persistence.auth;

import com.platform.domain.account.AccountActorType;
import com.platform.domain.auth.AuthSession;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuthSessionRepository extends JpaRepository<AuthSession, String> {

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select session from AuthSession session where session.id = :id")
	Optional<AuthSession> findByIdForUpdate(@Param("id") String id);

	boolean existsByIdAndActorTypeAndAccountIdAndRevokedAtIsNullAndExpiresAtAfter(
		String id,
		AccountActorType actorType,
		Long accountId,
		LocalDateTime now
	);

	List<AuthSession> findByActorTypeAndAccountIdAndRevokedAtIsNullAndExpiresAtAfterOrderByLastUsedAtDesc(
		AccountActorType actorType,
		Long accountId,
		LocalDateTime now
	);

	@Modifying
	@Query("""
		update AuthSession session
		set session.revokedAt = :now, session.revocationReason = :reason
		where session.actorType = :actorType
		  and session.accountId = :accountId
		  and session.revokedAt is null
		""")
	int revokeAll(
		@Param("actorType") AccountActorType actorType,
		@Param("accountId") Long accountId,
		@Param("now") LocalDateTime now,
		@Param("reason") String reason
	);

	@Modifying
	@Query("""
		delete from AuthSession session
		where session.expiresAt < :expiredBefore
		   or (session.revokedAt is not null and session.revokedAt < :revokedBefore)
		""")
	int deleteExpiredAndOldRevoked(
		@Param("expiredBefore") LocalDateTime expiredBefore,
		@Param("revokedBefore") LocalDateTime revokedBefore
	);
}
