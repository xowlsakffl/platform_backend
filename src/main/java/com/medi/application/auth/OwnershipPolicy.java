package com.medi.application.auth;

import com.medi.common.error.ApiException;
import com.medi.common.error.ErrorCode;
import com.medi.common.security.AuthenticatedActor;
import com.medi.domain.account.AccountActorType;
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

	public void requireBeautyOwner(AuthenticatedActor actor, Long beautyId) {
		permissionService.requireActor(actor, AccountActorType.BEAUTY);
		if (!Objects.equals(actor.beautyId(), beautyId)) {
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
