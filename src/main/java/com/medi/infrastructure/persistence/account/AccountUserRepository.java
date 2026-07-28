package com.medi.infrastructure.persistence.account;

import com.medi.domain.account.AccountUser;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountUserRepository extends JpaRepository<AccountUser, Long> {

	Optional<AccountUser> findByEmailAndDeletedAtIsNull(String email);

	Optional<AccountUser> findByIdAndDeletedAtIsNull(Long id);
}
