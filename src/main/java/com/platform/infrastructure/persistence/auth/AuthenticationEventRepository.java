package com.platform.infrastructure.persistence.auth;

import com.platform.domain.account.AccountActorType;
import com.platform.domain.auth.AuthenticationEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthenticationEventRepository extends JpaRepository<AuthenticationEvent, Long> {

	Page<AuthenticationEvent> findByActorTypeAndAccountId(
		AccountActorType actorType,
		Long accountId,
		Pageable pageable
	);
}
