package com.medi.application.auth;

import com.medi.common.error.ApiException;
import com.medi.common.error.ErrorCode;
import com.medi.common.security.AuthenticatedActor;
import com.medi.domain.account.AccountActorType;
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
