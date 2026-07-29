package com.medi.infrastructure.redis;

import com.medi.common.security.RevokedTokenStore;
import java.time.Duration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisRevokedTokenStore implements RevokedTokenStore {

	private static final String KEY_PREFIX = "auth:revoked:";

	private final StringRedisTemplate redisTemplate;

	public RedisRevokedTokenStore(StringRedisTemplate redisTemplate) {
		this.redisTemplate = redisTemplate;
	}

	@Override
	public boolean isRevoked(String tokenId) {
		return Boolean.TRUE.equals(redisTemplate.hasKey(KEY_PREFIX + tokenId));
	}

	@Override
	public void revoke(String tokenId, Duration ttl) {
		if (ttl.isZero() || ttl.isNegative()) {
			return;
		}
		redisTemplate.opsForValue().set(KEY_PREFIX + tokenId, "1", ttl);
	}
}
