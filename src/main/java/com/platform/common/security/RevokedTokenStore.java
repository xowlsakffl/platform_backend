package com.platform.common.security;

import java.time.Duration;

public interface RevokedTokenStore {

	boolean isRevoked(String tokenId);

	void revoke(String tokenId, Duration ttl);
}
