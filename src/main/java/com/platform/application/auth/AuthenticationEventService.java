package com.platform.application.auth;

import com.platform.application.auth.command.AuthClientContext;
import com.platform.domain.account.AccountActorType;
import com.platform.domain.auth.AuthenticationEvent;
import com.platform.domain.auth.AuthenticationEventResult;
import com.platform.infrastructure.persistence.auth.AuthenticationEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthenticationEventService {

	private final AuthenticationEventRepository repository;

	public AuthenticationEventService(AuthenticationEventRepository repository) {
		this.repository = repository;
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void record(
		AccountActorType actorType,
		Long accountId,
		AuthenticationEventResult result,
		String failureCode,
		AuthClientContext client
	) {
		repository.save(AuthenticationEvent.create(
			actorType,
			accountId,
			result,
			failureCode,
			client.ipAddress(),
			client.userAgent()
		));
	}
}
