package com.medi.infrastructure.persistence.auth;

import com.medi.domain.account.AccountActorType;
import com.medi.domain.auth.PasswordResetToken;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

	Optional<PasswordResetToken> findByActorTypeAndEmail(AccountActorType actorType, String email);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
		select token from PasswordResetToken token
		where token.actorType = :actorType and token.email = :email
		""")
	Optional<PasswordResetToken> findForUpdate(
		@Param("actorType") AccountActorType actorType,
		@Param("email") String email
	);

	@Modifying
	@Query("delete from PasswordResetToken token where token.expiresAt < :now")
	int deleteExpired(@Param("now") LocalDateTime now);
}
