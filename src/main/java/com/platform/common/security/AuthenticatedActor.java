package com.platform.common.security;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.platform.domain.account.AccountActorType;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

public record AuthenticatedActor(
	@JsonProperty("actor_type")
	AccountActorType actorType,
	@JsonProperty("account_id")
	Long accountId,
	@JsonProperty("partner_id")
	Long partnerId,
	@JsonProperty("session_id")
	String sessionId,
	String email,
	String name,
	String nickname,
	Set<String> permissions
) {

	public AuthenticatedActor {
		permissions = permissions == null ? Set.of() : Set.copyOf(permissions);
	}

	public Collection<GrantedAuthority> authorities() {
		Set<GrantedAuthority> authorities = new LinkedHashSet<>();
		authorities.add(new SimpleGrantedAuthority("ACTOR_" + actorType.name()));
		for (String permission : permissions) {
			authorities.add(new SimpleGrantedAuthority(permission));
		}
		return authorities;
	}

	public AuthenticatedActor withSessionId(String value) {
		return new AuthenticatedActor(
			actorType,
			accountId,
			partnerId,
			value,
			email,
			name,
			nickname,
			permissions
		);
	}
}
