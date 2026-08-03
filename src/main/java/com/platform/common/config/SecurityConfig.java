package com.platform.common.config;

import com.platform.common.security.ApiSecurityExceptionHandler;
import com.platform.common.security.AuthCookieRequestFilter;
import com.platform.common.security.AuthSessionProperties;
import com.platform.common.security.JwtAuthenticationFilter;
import com.platform.common.security.JwtProperties;
import com.platform.common.security.LoginAttemptProperties;
import com.platform.common.security.PasswordResetProperties;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.proc.SecurityContext;
import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableConfigurationProperties({
	JwtProperties.class,
	AuthSessionProperties.class,
	LoginAttemptProperties.class,
	PasswordResetProperties.class,
	CorsProperties.class
})
class SecurityConfig {

	@Bean
	SecurityFilterChain securityFilterChain(
		HttpSecurity http,
		ApiSecurityExceptionHandler apiSecurityExceptionHandler,
		AuthCookieRequestFilter authCookieRequestFilter,
		JwtAuthenticationFilter jwtAuthenticationFilter
	) throws Exception {
		return http
			.cors(Customizer.withDefaults())
			.csrf(AbstractHttpConfigurer::disable)
			.formLogin(AbstractHttpConfigurer::disable)
			.httpBasic(AbstractHttpConfigurer::disable)
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.authorizeHttpRequests(authorize -> authorize
				.requestMatchers(
					"/api/v1/public/**",
					"/api/v1/staff/auth/login",
					"/api/v1/staff/auth/refresh",
					"/api/v1/partner/auth/login",
					"/api/v1/partner/auth/refresh",
					"/api/v1/partner/onboarding/signup",
					"/api/v1/user/auth/login",
					"/api/v1/user/auth/refresh",
					"/api/v1/*/auth/password-reset-link",
					"/api/v1/*/auth/password-reset/verify",
					"/api/v1/*/auth/password-reset",
					"/api/v1/user/media/*/content",
					"/actuator/health"
				).permitAll()
				.requestMatchers("/api/v1/staff/**").hasAuthority("ACTOR_STAFF")
				.requestMatchers("/api/v1/partner/**").hasAuthority("ACTOR_PARTNER")
				.requestMatchers("/api/v1/user/**").hasAuthority("ACTOR_USER")
				.anyRequest().authenticated())
			.exceptionHandling(exceptionHandling -> exceptionHandling
				.authenticationEntryPoint(apiSecurityExceptionHandler)
				.accessDeniedHandler(apiSecurityExceptionHandler))
			.addFilterBefore(authCookieRequestFilter, UsernamePasswordAuthenticationFilter.class)
			.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
			.build();
	}

	@Bean
	CorsConfigurationSource corsConfigurationSource(CorsProperties properties) {
		CorsConfiguration configuration = new CorsConfiguration();
		configuration.setAllowedOrigins(properties.allowedOrigins());
		configuration.setAllowedMethods(
			List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
		);
		configuration.setAllowedHeaders(
			List.of("Authorization", "Content-Type", "Accept", "X-Request-Id", "X-Auth-Request")
		);
		configuration.setExposedHeaders(List.of("X-Request-Id", "Content-Disposition", "Retry-After"));
		configuration.setAllowCredentials(true);
		configuration.setMaxAge(3600L);

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/api/**", configuration);
		return source;
	}

	@Bean
	JwtEncoder jwtEncoder(JwtProperties properties) {
		SecretKey key = signingKey(properties);
		return new NimbusJwtEncoder(new ImmutableSecret<SecurityContext>(key));
	}

	@Bean
	JwtDecoder jwtDecoder(JwtProperties properties) {
		SecretKey key = signingKey(properties);
		NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(key)
			.macAlgorithm(MacAlgorithm.HS256)
			.build();

		OAuth2TokenValidator<Jwt> issuerValidator = JwtValidators.createDefaultWithIssuer(properties.issuer());
		OAuth2TokenValidator<Jwt> audienceValidator = token -> {
			if (token.getAudience().contains(properties.audience())) {
				return OAuth2TokenValidatorResult.success();
			}
			OAuth2Error error = new OAuth2Error("invalid_token", "JWT audience가 올바르지 않습니다.", null);
			return OAuth2TokenValidatorResult.failure(error);
		};
		decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(issuerValidator, audienceValidator));
		return decoder;
	}

	private SecretKey signingKey(JwtProperties properties) {
		return new SecretKeySpec(properties.secret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	UserDetailsService userDetailsService() {
		return username -> {
			throw new UsernameNotFoundException(username);
		};
	}
}
