package com.platform.common.security;

import com.platform.common.error.ApiException;
import com.platform.common.error.ErrorCode;
import com.platform.domain.account.AccountActorType;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;

@Service
public class JwtTokenService {

	private final JwtEncoder jwtEncoder;
	private final JwtDecoder jwtDecoder;
	private final JwtProperties properties;
	private final RevokedTokenStore revokedTokenStore;

	public JwtTokenService(
		JwtEncoder jwtEncoder,
		JwtDecoder jwtDecoder,
		JwtProperties properties,
		RevokedTokenStore revokedTokenStore
	) {
		this.jwtEncoder = jwtEncoder;
		this.jwtDecoder = jwtDecoder;
		this.properties = properties;
		this.revokedTokenStore = revokedTokenStore;
	}

	public String issue(AuthenticatedActor actor) {
		Instant now = Instant.now();
		Instant expiresAt = now.plusSeconds(properties.accessTokenTtlSeconds());
		JwtClaimsSet.Builder claimsBuilder = JwtClaimsSet.builder()
			.issuer(properties.issuer())
			.audience(List.of(properties.audience()))
			.subject(actor.actorType().name() + ":" + actor.accountId())
			.id(UUID.randomUUID().toString())
			.issuedAt(now)
			.notBefore(now)
			.expiresAt(expiresAt)
			.claim("sid", actor.sessionId())
			.claim("actor", actor.actorType().name())
			.claim("account_id", actor.accountId())
			.claim("permissions", actor.permissions().stream().sorted().toList());
		optionalClaim(claimsBuilder, "partner_id", actor.partnerId());
		optionalClaim(claimsBuilder, "email", actor.email());
		optionalClaim(claimsBuilder, "login_id", actor.loginId());
		optionalClaim(claimsBuilder, "name", actor.name());
		optionalClaim(claimsBuilder, "nickname", actor.nickname());
		JwtClaimsSet claims = claimsBuilder.build();
		JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).type("JWT").build();
		return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
	}

	public AuthenticatedActor parse(String token) {
		Jwt jwt = decode(token);
		if (revokedTokenStore.isRevoked(jwt.getId())) {
			throw unauthorized();
		}

		try {
			return new AuthenticatedActor(
				AccountActorType.valueOf(jwt.getClaimAsString("actor")),
				jwt.getClaim("account_id"),
				jwt.getClaim("partner_id"),
				jwt.getClaimAsString("sid"),
				jwt.getClaimAsString("email"),
				jwt.getClaimAsString("login_id"),
				jwt.getClaimAsString("name"),
				jwt.getClaimAsString("nickname"),
				permissions(jwt)
			);
		} catch (IllegalArgumentException | ClassCastException exception) {
			throw unauthorized();
		}
	}

	public void revoke(String token) {
		Jwt jwt = decode(token);
		Instant expiresAt = jwt.getExpiresAt();
		if (expiresAt == null || jwt.getId() == null) {
			throw unauthorized();
		}
		Duration remaining = Duration.between(Instant.now(), expiresAt);
		if (!remaining.isNegative() && !remaining.isZero()) {
			revokedTokenStore.revoke(jwt.getId(), remaining);
		}
	}

	public long accessTokenTtlSeconds() {
		return properties.accessTokenTtlSeconds();
	}

	private void optionalClaim(JwtClaimsSet.Builder builder, String name, Object value) {
		if (value != null) {
			builder.claim(name, value);
		}
	}

	private Jwt decode(String token) {
		if (token == null || token.isBlank()) {
			throw unauthorized();
		}
		try {
			return jwtDecoder.decode(token);
		} catch (JwtException exception) {
			throw unauthorized();
		}
	}

	private Set<String> permissions(Jwt jwt) {
		List<String> values = jwt.getClaimAsStringList("permissions");
		return values == null ? Set.of() : Set.copyOf(values);
	}

	private ApiException unauthorized() {
		return new ApiException(ErrorCode.UNAUTHORIZED, "인증 토큰이 유효하지 않습니다.");
	}
}
