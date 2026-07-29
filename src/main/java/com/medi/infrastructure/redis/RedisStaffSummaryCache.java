package com.medi.infrastructure.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.medi.application.cache.StaffSummaryCache;
import java.time.Duration;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisStaffSummaryCache implements StaffSummaryCache {

	private static final Logger log = LoggerFactory.getLogger(RedisStaffSummaryCache.class);
	private static final String KEY_PREFIX = "staff:summary:";

	private final StringRedisTemplate redisTemplate;
	private final ObjectMapper objectMapper;
	private final Duration ttl;

	public RedisStaffSummaryCache(
		StringRedisTemplate redisTemplate,
		ObjectMapper objectMapper,
		@Value("${app.cache.staff-summary-ttl-seconds:300}") long ttlSeconds
	) {
		this.redisTemplate = redisTemplate;
		this.objectMapper = objectMapper;
		this.ttl = Duration.ofSeconds(ttlSeconds > 0 ? ttlSeconds : 300);
	}

	@Override
	public <T> T remember(String domain, Class<T> resultType, Supplier<T> resolver) {
		String key = key(domain);
		try {
			String cached = redisTemplate.opsForValue().get(key);
			if (cached != null) {
				return objectMapper.readValue(cached, resultType);
			}
		} catch (RuntimeException | JsonProcessingException exception) {
			log.warn("Staff summary 캐시 조회에 실패해 DB로 대체합니다. domain={}", domain, exception);
		}

		T resolved = resolver.get();
		try {
			redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(resolved), ttl);
		} catch (RuntimeException | JsonProcessingException exception) {
			log.warn("Staff summary 캐시 저장에 실패했습니다. domain={}", domain, exception);
		}
		return resolved;
	}

	@Override
	public void forget(String domain) {
		try {
			redisTemplate.delete(key(domain));
		} catch (RuntimeException exception) {
			log.warn("Staff summary 캐시 삭제에 실패했습니다. domain={}", domain, exception);
		}
	}

	private String key(String domain) {
		return KEY_PREFIX + domain + ":default";
	}
}
