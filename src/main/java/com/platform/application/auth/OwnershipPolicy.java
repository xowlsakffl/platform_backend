package com.platform.application.auth;

import com.platform.common.error.ApiException;
import com.platform.common.error.ErrorCode;
import com.platform.common.security.AuthenticatedActor;
import com.platform.domain.account.AccountActorType;
import com.platform.domain.partner.PartnerMembershipStatus;
import com.platform.infrastructure.persistence.partner.PartnerMembershipRepository;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class OwnershipPolicy {

	private final PermissionService permissionService;
	private final PartnerMembershipRepository membershipRepository;

	public OwnershipPolicy(
		PermissionService permissionService,
		PartnerMembershipRepository membershipRepository
	) {
		this.permissionService = permissionService;
		this.membershipRepository = membershipRepository;
	}

	public void requirePartnerOwner(AuthenticatedActor actor, Long partnerId) {
		permissionService.requireActor(actor, AccountActorType.PARTNER);
		if (partnerId == null || !membershipRepository.existsByAccountPartner_IdAndPartner_IdAndStatus(
			actor.accountId(),
			partnerId,
			PartnerMembershipStatus.ACTIVE
		)) {
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
