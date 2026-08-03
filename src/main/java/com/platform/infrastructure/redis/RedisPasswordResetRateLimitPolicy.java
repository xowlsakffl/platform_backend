package com.platform.infrastructure.redis;

import com.platform.application.auth.PasswordResetRateLimitPolicy;
import com.platform.common.error.ApiException;
import com.platform.common.error.ErrorCode;
import com.platform.common.error.RateLimitException;
import com.platform.common.security.PasswordResetProperties;
import com.platform.domain.account.AccountActorType;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisPasswordResetRateLimitPolicy implements PasswordResetRateLimitPolicy {

	private static final Logger log = LoggerFactory.getLogger(RedisPasswordResetRateLimitPolicy.class);
	private static final String KEY_PREFIX = "auth:password-reset:";

	private final StringRedisTemplate redisTemplate;
	private final PasswordResetProperties properties;

	public RedisPasswordResetRateLimitPolicy(
		StringRedisTemplate redisTemplate,
		PasswordResetProperties properties
	) {
		this.redisTemplate = redisTemplate;
		this.properties = properties;
	}

	@Override
	public void checkLinkRequest(AccountActorType actorType, String email, String ipAddress) {
		check(
			"request:ip:" + digest(actorType.name() + ":" + normalizedIp(ipAddress)),
			properties.requestIpMaxAttempts(),
			properties.requestWindowSeconds()
		);
		check(
			"request:email:" + digest(actorType.name() + ":" + email),
			properties.requestEmailMaxAttempts(),
			properties.requestWindowSeconds()
		);
	}

	@Override
	public void checkTokenVerification(AccountActorType actorType, String email, String ipAddress) {
		check(
			"verify:" + digest(actorType.name() + ":" + email + ":" + normalizedIp(ipAddress)),
			properties.verifyMaxAttempts(),
			properties.verifyWindowSeconds()
		);
	}

	@Override
	public void checkPasswordReset(AccountActorType actorType, String email, String ipAddress) {
		check(
			"submit:" + digest(actorType.name() + ":" + email + ":" + normalizedIp(ipAddress)),
			properties.submitMaxAttempts(),
			properties.submitWindowSeconds()
		);
	}

	private void check(String suffix, int maxAttempts, long windowSeconds) {
		try {
			String key = KEY_PREFIX + suffix;
			Long count = redisTemplate.opsForValue().increment(key);
			if (count == null) {
				throw new IllegalStateException("Redis increment 결과가 없습니다.");
			}
			if (count == 1) {
				redisTemplate.expire(key, Duration.ofSeconds(windowSeconds));
			}
			long retryAfterSeconds = retryAfterSeconds(key, windowSeconds);
			if (count > maxAttempts) {
				throw new RateLimitException("비밀번호 재설정 요청이 너무 많습니다.", retryAfterSeconds);
			}
		} catch (ApiException exception) {
			throw exception;
		} catch (RuntimeException exception) {
			log.error("비밀번호 재설정 요청 제한 상태를 기록할 수 없습니다.", exception);
			throw new ApiException(ErrorCode.INTERNAL_ERROR, "비밀번호 재설정 보안 상태를 확인할 수 없습니다.");
		}
	}

	private long retryAfterSeconds(String key, long windowSeconds) {
		Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
		if (ttl != null && ttl > 0) {
			return ttl;
		}
		redisTemplate.expire(key, Duration.ofSeconds(windowSeconds));
		return windowSeconds;
	}

	private String normalizedIp(String value) {
		return value == null || value.isBlank() ? "unknown" : value.trim();
	}

	private String digest(String value) {
		try {
			byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(digest);
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("SHA-256 알고리즘을 사용할 수 없습니다.", exception);
		}
	}
}
