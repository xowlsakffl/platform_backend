package com.medi.application.auth.result;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.medi.common.security.AuthenticatedActor;
import com.medi.domain.account.AccountActorType;
import java.util.List;

public record AuthActorResult(
	@JsonProperty("actor_type")
	AccountActorType actorType,
	@JsonProperty("account_id")
	Long accountId,
	@JsonProperty("hospital_id")
	Long hospitalId,
	@JsonProperty("beauty_id")
	Long beautyId,
	String email,
	String name,
	String nickname,
	List<String> permissions
) {

	public static AuthActorResult from(AuthenticatedActor actor) {
		return new AuthActorResult(
			actor.actorType(),
			actor.accountId(),
			actor.hospitalId(),
			actor.beautyId(),
			actor.email(),
			actor.name(),
			actor.nickname(),
			actor.permissions().stream().sorted().toList()
		);
	}
}
