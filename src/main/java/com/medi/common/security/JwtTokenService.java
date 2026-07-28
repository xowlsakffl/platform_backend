package com.medi.common.security;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.medi.common.error.ApiException;
import com.medi.common.error.ErrorCode;
import com.medi.domain.account.AccountActorType;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Service;

@Service
public class JwtTokenService {

	private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
	private static final Base64.Decoder DECODER = Base64.getUrlDecoder();
	private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
	};

	private final ObjectMapper objectMapper;
	private final JwtProperties properties;

	public JwtTokenService(ObjectMapper objectMapper, JwtProperties properties) {
		this.objectMapper = objectMapper;
		this.properties = properties;
	}

	public String issue(AuthenticatedActor actor) {
		Instant now = Instant.now();
		Instant expiresAt = now.plusSeconds(properties.accessTokenTtlSeconds());

		Map<String, Object> header = Map.of(
			"alg", "HS256",
			"typ", "JWT"
		);
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("iss", properties.issuer());
		payload.put("sub", actor.actorType().name() + ":" + actor.accountId());
		payload.put("jti", UUID.randomUUID().toString());
		payload.put("iat", now.getEpochSecond());
		payload.put("exp", expiresAt.getEpochSecond());
		payload.put("actor", actor.actorType().name());
		payload.put("account_id", actor.accountId());
		payload.put("hospital_id", actor.hospitalId());
		payload.put("beauty_id", actor.beautyId());
		payload.put("email", actor.email());
		payload.put("name", actor.name());
		payload.put("nickname", actor.nickname());
		payload.put("permissions", actor.permissions().stream().sorted().toList());

		String unsignedToken = encodeJson(header) + "." + encodeJson(payload);
		return unsignedToken + "." + sign(unsignedToken);
	}

	public AuthenticatedActor parse(String token) {
		String[] parts = token.split("\\.");
		if (parts.length != 3) {
			throw unauthorized();
		}

		String unsignedToken = parts[0] + "." + parts[1];
		if (!MessageDigest.isEqual(sign(unsignedToken).getBytes(StandardCharsets.UTF_8), parts[2].getBytes(StandardCharsets.UTF_8))) {
			throw unauthorized();
		}

		Map<String, Object> header = decodeJson(parts[0]);
		if (!"HS256".equals(header.get("alg"))) {
			throw unauthorized();
		}

		Map<String, Object> payload = decodeJson(parts[1]);
		if (!properties.issuer().equals(payload.get("iss"))) {
			throw unauthorized();
		}
		if (longValue(payload.get("exp")) < Instant.now().getEpochSecond()) {
			throw unauthorized();
		}

		AccountActorType actorType = actorType(payload.get("actor"));
		return new AuthenticatedActor(
			actorType,
			longValue(payload.get("account_id")),
			nullableLong(payload.get("hospital_id")),
			nullableLong(payload.get("beauty_id")),
			stringValue(payload.get("email")),
			stringValue(payload.get("name")),
			stringValue(payload.get("nickname")),
			permissions(payload.get("permissions"))
		);
	}

	public long accessTokenTtlSeconds() {
		return properties.accessTokenTtlSeconds();
	}

	private String encodeJson(Map<String, Object> value) {
		try {
			return ENCODER.encodeToString(objectMapper.writeValueAsBytes(value));
		} catch (JsonProcessingException exception) {
			throw new ApiException(ErrorCode.INTERNAL_ERROR);
		}
	}

	private Map<String, Object> decodeJson(String value) {
		try {
			return objectMapper.readValue(DECODER.decode(value), MAP_TYPE);
		} catch (IllegalArgumentException | IOException exception) {
			throw unauthorized();
		}
	}

	private String sign(String value) {
		try {
			Mac mac = Mac.getInstance("HmacSHA256");
			mac.init(new SecretKeySpec(properties.secret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
			return ENCODER.encodeToString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
		} catch (Exception exception) {
			throw new ApiException(ErrorCode.INTERNAL_ERROR);
		}
	}

	private AccountActorType actorType(Object value) {
		try {
			return AccountActorType.valueOf(stringValue(value));
		} catch (IllegalArgumentException exception) {
			throw unauthorized();
		}
	}

	private Long nullableLong(Object value) {
		if (value == null) {
			return null;
		}
		return longValue(value);
	}

	private long longValue(Object value) {
		if (value instanceof Number number) {
			return number.longValue();
		}
		if (value instanceof String string && !string.isBlank()) {
			return Long.parseLong(string);
		}
		throw unauthorized();
	}

	private String stringValue(Object value) {
		if (value instanceof String string) {
			return string;
		}
		throw unauthorized();
	}

	private Set<String> permissions(Object value) {
		if (!(value instanceof List<?> list)) {
			return Set.of();
		}
		Set<String> permissions = new LinkedHashSet<>();
		for (Object item : list) {
			if (item instanceof String permission && !permission.isBlank()) {
				permissions.add(permission);
			}
		}
		return permissions;
	}

	private ApiException unauthorized() {
		return new ApiException(ErrorCode.UNAUTHORIZED, "인증 토큰이 유효하지 않습니다.");
	}
}
