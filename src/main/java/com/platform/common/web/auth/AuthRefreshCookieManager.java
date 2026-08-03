package com.platform.common.web.auth;

import com.platform.common.security.AuthSessionProperties;
import com.platform.common.error.ApiException;
import com.platform.common.error.ErrorCode;
import com.platform.domain.account.AccountActorType;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class AuthRefreshCookieManager {

	private final AuthSessionProperties properties;

	public AuthRefreshCookieManager(AuthSessionProperties properties) {
		this.properties = properties;
	}

	@PostConstruct
	void validate() {
		String sameSite = normalizedSameSite();
		if (!sameSite.equals("Strict") && !sameSite.equals("Lax") && !sameSite.equals("None")) {
			throw new IllegalStateException("AUTH_COOKIE_SAME_SITE는 Strict, Lax, None 중 하나여야 합니다.");
		}
		if (sameSite.equals("None") && !properties.cookieSecure()) {
			throw new IllegalStateException("SameSite=None 쿠키는 Secure 설정이 필요합니다.");
		}
	}

	public Optional<String> read(AccountActorType actorType, HttpServletRequest request) {
		Cookie[] cookies = request.getCookies();
		if (cookies == null) {
			return Optional.empty();
		}
		String name = cookieName(actorType);
		return Arrays.stream(cookies)
			.filter(cookie -> name.equals(cookie.getName()))
			.map(Cookie::getValue)
			.filter(value -> !value.isBlank())
			.findFirst();
	}

	public String require(AccountActorType actorType, HttpServletRequest request) {
		return read(actorType, request).orElseThrow(() ->
			new ApiException(ErrorCode.TOKEN_ERROR, "리프레시 토큰이 없습니다."));
	}

	public void write(
		AccountActorType actorType,
		String refreshToken,
		long expiresIn,
		boolean persistent,
		HttpServletResponse response
	) {
		ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(cookieName(actorType), refreshToken)
			.httpOnly(true)
			.secure(properties.cookieSecure())
			.sameSite(normalizedSameSite())
			.path(cookiePath(actorType));
		if (persistent) {
			builder.maxAge(Duration.ofSeconds(expiresIn));
		}
		response.addHeader(HttpHeaders.SET_COOKIE, builder.build().toString());
	}

	public void clear(AccountActorType actorType, HttpServletResponse response) {
		ResponseCookie cookie = ResponseCookie.from(cookieName(actorType), "")
			.httpOnly(true)
			.secure(properties.cookieSecure())
			.sameSite(normalizedSameSite())
			.path(cookiePath(actorType))
			.maxAge(Duration.ZERO)
			.build();
		response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
	}

	private String cookieName(AccountActorType actorType) {
		return "platform_refresh_" + actorType.name().toLowerCase(Locale.ROOT);
	}

	private String cookiePath(AccountActorType actorType) {
		return "/api/v1/" + actorType.name().toLowerCase(Locale.ROOT) + "/auth";
	}

	private String normalizedSameSite() {
		String value = properties.cookieSameSite().trim().toLowerCase(Locale.ROOT);
		return Character.toUpperCase(value.charAt(0)) + value.substring(1);
	}
}
