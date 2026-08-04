package com.platform.domain.partner;

import com.platform.domain.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "partner_account_invitations")
public class PartnerAccountInvitation extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "partner_id", nullable = false)
	private Partner partner;

	@Column(nullable = false)
	private String email;

	@Column(name = "token_hash", nullable = false, unique = true, length = 64)
	private String tokenHash;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private PartnerAccountInvitationStatus status = PartnerAccountInvitationStatus.PENDING;

	@Column(name = "expires_at", nullable = false)
	private LocalDateTime expiresAt;

	@Column(name = "sent_at")
	private LocalDateTime sentAt;

	@Column(name = "accepted_at")
	private LocalDateTime acceptedAt;

	@Column(name = "canceled_at")
	private LocalDateTime canceledAt;

	@Column(name = "created_by_staff_id")
	private Long createdByStaffId;

	protected PartnerAccountInvitation() {
	}

	public static PartnerAccountInvitation create(
		Partner partner,
		String email,
		String tokenHash,
		LocalDateTime expiresAt,
		Long createdByStaffId
	) {
		PartnerAccountInvitation invitation = new PartnerAccountInvitation();
		invitation.partner = Objects.requireNonNull(partner);
		invitation.email = Objects.requireNonNull(email);
		invitation.tokenHash = Objects.requireNonNull(tokenHash);
		invitation.expiresAt = Objects.requireNonNull(expiresAt);
		invitation.createdByStaffId = createdByStaffId;
		return invitation;
	}

	public void markSent(LocalDateTime sentAt) {
		if (status != PartnerAccountInvitationStatus.PENDING) {
			throw new IllegalStateException("Only a pending invitation can be marked as sent.");
		}
		this.sentAt = Objects.requireNonNull(sentAt);
	}

	public void accept(LocalDateTime acceptedAt) {
		if (status != PartnerAccountInvitationStatus.PENDING || isExpired(acceptedAt)) {
			throw new IllegalStateException("The invitation is not acceptable.");
		}
		this.status = PartnerAccountInvitationStatus.ACCEPTED;
		this.acceptedAt = Objects.requireNonNull(acceptedAt);
	}

	public void cancel(LocalDateTime canceledAt) {
		if (status == PartnerAccountInvitationStatus.ACCEPTED) {
			throw new IllegalStateException("An accepted invitation cannot be canceled.");
		}
		this.status = PartnerAccountInvitationStatus.CANCELED;
		this.canceledAt = Objects.requireNonNull(canceledAt);
	}

	public boolean isExpired(LocalDateTime now) {
		return !expiresAt.isAfter(now);
	}

	public Long id() {
		return id;
	}

	public Partner partner() {
		return partner;
	}

	public Long partnerId() {
		return partner == null ? null : partner.id();
	}

	public String email() {
		return email;
	}

	public String tokenHash() {
		return tokenHash;
	}

	public PartnerAccountInvitationStatus status() {
		return status;
	}

	public LocalDateTime expiresAt() {
		return expiresAt;
	}

	public LocalDateTime sentAt() {
		return sentAt;
	}

	public LocalDateTime acceptedAt() {
		return acceptedAt;
	}

	public LocalDateTime canceledAt() {
		return canceledAt;
	}

	public Long createdByStaffId() {
		return createdByStaffId;
	}
}
