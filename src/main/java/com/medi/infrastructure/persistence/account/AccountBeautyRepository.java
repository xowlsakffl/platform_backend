package com.medi.infrastructure.persistence.account;

import com.medi.domain.account.AccountBeauty;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountBeautyRepository extends JpaRepository<AccountBeauty, Long> {

	Optional<AccountBeauty> findByEmailAndDeletedAtIsNull(String email);

	Optional<AccountBeauty> findByIdAndDeletedAtIsNull(Long id);
}
