package com.platform.application.auth;

import com.platform.common.error.ApiException;
import com.platform.common.error.ErrorCode;
import com.platform.common.security.AuthenticatedActor;
import com.platform.domain.account.AccountActorType;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class OwnershipPolicy {

	private final PermissionService permissionService;

	public OwnershipPolicy(PermissionService permissionService) {
		this.permissionService = permissionService;
	}

	public void requirePartnerOwner(AuthenticatedActor actor, Long partnerId) {
		permissionService.requireActor(actor, AccountActorType.PARTNER);
		if (!Objects.equals(actor.partnerId(), partnerId)) {
			throw new ApiException(ErrorCode.FORBIDDEN);
		}
	}

	public void requireUserSelf(AuthenticatedActor actor, Long accountUserId) {
		permissionService.requireActor(actor, AccountActorType.USER);
		if (!Objects.equals(actor.accountId(), accountUserId)) {
			throw new ApiException(ErrorCode.FORBIDDEN);
		}
	}
}
