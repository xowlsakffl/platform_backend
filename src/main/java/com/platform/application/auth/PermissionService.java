package com.platform.application.auth;

import com.platform.common.error.ApiException;
import com.platform.common.error.ErrorCode;
import com.platform.common.security.AuthenticatedActor;
import com.platform.domain.account.AccountActorType;
import org.springframework.stereotype.Service;

@Service
public class PermissionService {

	public void requireStaffPermission(AuthenticatedActor actor, String permission) {
		if (!hasStaffPermission(actor, permission)) {
			throw new ApiException(ErrorCode.FORBIDDEN);
		}
	}

	public boolean hasStaffPermission(AuthenticatedActor actor, String permission) {
		requireActor(actor, AccountActorType.STAFF);
		return actor.permissions().contains(permission);
	}

	public void requireActor(AuthenticatedActor actor, AccountActorType actorType) {
		if (actor == null) {
			throw new ApiException(ErrorCode.UNAUTHORIZED);
		}
		if (actor.actorType() != actorType) {
			throw new ApiException(ErrorCode.FORBIDDEN);
		}
	}
}
