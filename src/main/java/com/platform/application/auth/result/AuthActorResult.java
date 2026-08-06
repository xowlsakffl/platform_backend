package com.platform.application.auth.result;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.platform.common.security.AuthenticatedActor;
import com.platform.domain.account.AccountActorType;
import java.util.List;

public record AuthActorResult(
	@JsonProperty("actor_type")
	AccountActorType actorType,
	@JsonProperty("account_id")
	Long accountId,
	@JsonProperty("partner_id")
	Long partnerId,
	String email,
	@JsonProperty("login_id")
	String loginId,
	String name,
	String nickname,
	List<String> permissions
) {

	public static AuthActorResult from(AuthenticatedActor actor) {
		return new AuthActorResult(
			actor.actorType(),
			actor.accountId(),
			actor.partnerId(),
			actor.email(),
			actor.loginId(),
			actor.name(),
			actor.nickname(),
			actor.permissions().stream().sorted().toList()
		);
	}
}
