package com.platform.infrastructure.redis;

import com.platform.application.auth.LoginAttemptPolicy;
import com.platform.common.error.ApiException;
import com.platform.common.error.ErrorCode;
import com.platform.common.error.RateLimitException;
import com.platform.common.security.LoginAttemptProperties;
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
public class RedisLoginAttemptPolicy implements LoginAttemptPolicy {

	private static final Logger log = LoggerFactory.getLogger(RedisLoginAttemptPolicy.class);
	private static final String KEY_PREFIX = "auth:login-attempt:";

	private final StringRedisTemplate redisTemplate;
	private final LoginAttemptProperties properties;

	public RedisLoginAttemptPolicy(StringRedisTemplate redisTemplate, LoginAttemptProperties properties) {
		this.redisTemplate = redisTemplate;
		this.properties = properties;
	}

	@Override
	public void assertAllowed(AccountActorType actorType, String email, String ipAddress) {
		try {
			String pairKey = pairKey(actorType, email, ipAddress);
			String accountKey = accountKey(actorType, email);
			Long pairAttempts = count(pairKey);
			Long accountAttempts = count(accountKey);
			if (pairAttempts >= properties.maxAttempts() || accountAttempts >= properties.accountMaxAttempts()) {
				long retryAfterSeconds = 1;
				if (pairAttempts >= properties.maxAttempts()) {
					retryAfterSeconds = Math.max(retryAfterSeconds, retryAfterSeconds(pairKey));
				}
				if (accountAttempts >= properties.accountMaxAttempts()) {
					retryAfterSeconds = Math.max(retryAfterSeconds, retryAfterSeconds(accountKey));
				}
				throw new RateLimitException("로그인 시도가 너무 많습니다.", retryAfterSeconds);
			}
		} catch (ApiException exception) {
			throw exception;
		} catch (RuntimeException exception) {
			log.error("로그인 시도 제한 상태를 확인할 수 없습니다.", exception);
			throw new ApiException(ErrorCode.INTERNAL_ERROR, "로그인 보안 상태를 확인할 수 없습니다.");
		}
	}

	@Override
	public void recordFailure(AccountActorType actorType, String email, String ipAddress) {
		try {
			increment(pairKey(actorType, email, ipAddress));
			increment(accountKey(actorType, email));
		} catch (RuntimeException exception) {
			log.error("로그인 실패 횟수를 기록할 수 없습니다.", exception);
		}
	}

	@Override
	public void recordSuccess(AccountActorType actorType, String email, String ipAddress) {
		try {
			redisTemplate.delete(pairKey(actorType, email, ipAddress));
			redisTemplate.delete(accountKey(actorType, email));
		} catch (RuntimeException exception) {
			log.warn("로그인 실패 횟수를 초기화할 수 없습니다.", exception);
		}
	}

	private long count(String key) {
		String value = redisTemplate.opsForValue().get(key);
		return value == null ? 0 : Long.parseLong(value);
	}

	private void increment(String key) {
		Long count = redisTemplate.opsForValue().increment(key);
		if (count != null && count == 1) {
			redisTemplate.expire(key, Duration.ofSeconds(properties.windowSeconds()));
		}
		if (count != null) {
			retryAfterSeconds(key);
		}
	}

	private long retryAfterSeconds(String key) {
		Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
		if (ttl != null && ttl > 0) {
			return ttl;
		}
		redisTemplate.expire(key, Duration.ofSeconds(properties.windowSeconds()));
		return properties.windowSeconds();
	}

	private String pairKey(AccountActorType actorType, String email, String ipAddress) {
		return KEY_PREFIX + "pair:" + digest(actorType.name() + ":" + email + ":" + normalizedIp(ipAddress));
	}

	private String accountKey(AccountActorType actorType, String email) {
		return KEY_PREFIX + "account:" + digest(actorType.name() + ":" + email);
	}

	private String normalizedIp(String ipAddress) {
		return ipAddress == null || ipAddress.isBlank() ? "unknown" : ipAddress.trim();
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
