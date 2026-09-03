package com.platform.domain.auth;

import com.platform.domain.account.AccountActorType;
import com.platform.domain.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Objects;

@Entity
@Table(name = "authentication_events")
public class AuthenticationEvent extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Enumerated(EnumType.STRING)
	@Column(name = "actor_type", nullable = false, length = 20)
	private AccountActorType actorType;

	@Column(name = "account_id")
	private Long accountId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private AuthenticationEventResult result;

	@Column(name = "failure_code", length = 60)
	private String failureCode;

	@Column(name = "ip_address", length = 45)
	private String ipAddress;

	@Column(name = "user_agent", length = 500)
	private String userAgent;

	protected AuthenticationEvent() {
	}

	public static AuthenticationEvent create(
		AccountActorType actorType,
		Long accountId,
		AuthenticationEventResult result,
		String failureCode,
		String ipAddress,
		String userAgent
	) {
		AuthenticationEvent event = new AuthenticationEvent();
		event.actorType = Objects.requireNonNull(actorType);
		event.accountId = accountId;
		event.result = Objects.requireNonNull(result);
		event.failureCode = failureCode;
		event.ipAddress = ipAddress;
		event.userAgent = userAgent;
		return event;
	}

	public Long id() {
		return id;
	}

	public AccountActorType actorType() {
		return actorType;
	}

	public Long accountId() {
		return accountId;
	}

	public AuthenticationEventResult result() {
		return result;
	}

	public String failureCode() {
		return failureCode;
	}

	public String ipAddress() {
		return ipAddress;
	}

	public String userAgent() {
		return userAgent;
	}
}
