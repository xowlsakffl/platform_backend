package com.medi.common.security;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.medi.domain.account.AccountActorType;
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
	@JsonProperty("hospital_id")
	Long hospitalId,
	@JsonProperty("beauty_id")
	Long beautyId,
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
}
