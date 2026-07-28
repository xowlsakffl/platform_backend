package com.medi.common.config;

import com.medi.common.security.ApiSecurityExceptionHandler;
import com.medi.common.security.JwtAuthenticationFilter;
import com.medi.common.security.JwtProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableConfigurationProperties(JwtProperties.class)
class SecurityConfig {

	@Bean
	SecurityFilterChain securityFilterChain(
		HttpSecurity http,
		ApiSecurityExceptionHandler apiSecurityExceptionHandler,
		JwtAuthenticationFilter jwtAuthenticationFilter
	) throws Exception {
		return http
			.csrf(AbstractHttpConfigurer::disable)
			.formLogin(AbstractHttpConfigurer::disable)
			.httpBasic(AbstractHttpConfigurer::disable)
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.authorizeHttpRequests(authorize -> authorize
				.requestMatchers(
					"/api/v1/public/**",
					"/api/v1/staff/auth/login",
					"/api/v1/hospital/auth/login",
					"/api/v1/beauty/auth/login",
					"/api/v1/user/auth/login",
					"/actuator/health"
				).permitAll()
				.requestMatchers("/api/v1/staff/**").hasAuthority("ACTOR_STAFF")
				.requestMatchers("/api/v1/hospital/**").hasAuthority("ACTOR_HOSPITAL")
				.requestMatchers("/api/v1/beauty/**").hasAuthority("ACTOR_BEAUTY")
				.requestMatchers("/api/v1/user/**").hasAuthority("ACTOR_USER")
				.anyRequest().authenticated())
			.exceptionHandling(exceptionHandling -> exceptionHandling
				.authenticationEntryPoint(apiSecurityExceptionHandler)
				.accessDeniedHandler(apiSecurityExceptionHandler))
			.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
			.build();
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
